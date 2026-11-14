package com.orbi.orbi_app.data.remote;

import android.content.Context;
import android.text.TextUtils;

import com.orbi.orbi_app.data.SessionManager;

import java.net.ConnectException;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static Retrofit retrofit;
    private static OrbiApiService apiService;

    public static synchronized OrbiApiService getApiService(Context context) {
        SessionManager sessionManager = new SessionManager(context);
        String baseUrl = sessionManager.getServerUrl();

        if (apiService == null || retrofit == null || !retrofit.baseUrl().toString().equals(baseUrl)) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .writeTimeout(20, TimeUnit.SECONDS)
                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        HttpUrl url = original.url();
                        String host = url.host();
                        boolean emulator = SessionManager.isEmulator();

                        // Em dispositivo físico, 10.0.2.2 não existe e causaria timeout de 15s;
                        // tenta primeiro localhost (caso adb reverse esteja ativo).
                        if (!emulator && "10.0.2.2".equals(host)) {
                            url = url.newBuilder().host("localhost").build();
                            host = "localhost";
                        }

                        Request.Builder builder = original.newBuilder().url(url);

                        String token = sessionManager.getAuthToken();
                        if (!TextUtils.isEmpty(token)) {
                            builder.header("Authorization", "Bearer " + token);
                        }

                        builder.header("Content-Type", "application/json");
                        builder.header("Accept", "application/json");

                        Request request = builder.build();
                        try {
                            return chain.proceed(request);
                        } catch (ConnectException e) {
                            // Se localhost/127.0.0.1 recusar conexão, faz fallback automático:
                            // - Emulador -> 10.0.2.2 (loopback para o PC host)
                            // - Dispositivo físico -> IP da rede local (LAN)
                            if ("localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host)) {
                                String fallbackHost = emulator ? "10.0.2.2" : "192.168.1.60";
                                HttpUrl fallbackUrl = request.url().newBuilder().host(fallbackHost).build();
                                Request fallbackRequest = request.newBuilder().url(fallbackUrl).build();
                                return chain.proceed(fallbackRequest);
                            }
                            throw e;
                        }
                    })
                    .addInterceptor(loggingInterceptor)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            apiService = retrofit.create(OrbiApiService.class);
        }
        return apiService;
    }

    public static synchronized void setBaseUrl(String newUrl, Context context) {
        SessionManager sessionManager = new SessionManager(context);
        sessionManager.setServerUrl(newUrl);
        apiService = null;
        retrofit = null;
        com.orbi.orbi_app.data.RouteRepository.getInstance().resetForNewSession();
        getApiService(context);
    }

    public static String getBaseUrl(Context context) {
        return new SessionManager(context).getServerUrl();
    }
}
