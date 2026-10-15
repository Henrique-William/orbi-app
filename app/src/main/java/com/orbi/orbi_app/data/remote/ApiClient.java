package com.orbi.orbi_app.data.remote;

import android.content.Context;
import android.text.TextUtils;

import com.orbi.orbi_app.data.SessionManager;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
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
                        Request.Builder builder = original.newBuilder();

                        String token = sessionManager.getAuthToken();
                        if (!TextUtils.isEmpty(token)) {
                            builder.header("Authorization", "Bearer " + token);
                        }

                        builder.header("Content-Type", "application/json");
                        builder.header("Accept", "application/json");

                        return chain.proceed(builder.build());
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
        getApiService(context);
    }

    public static String getBaseUrl(Context context) {
        return new SessionManager(context).getServerUrl();
    }
}
