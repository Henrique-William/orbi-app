package com.orbi.orbi_app.data;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AddressGeocoder {

    public interface GeocodeCallback {
        void onGeocoded(double latitude, double longitude);
        void onError(String error);
    }

    private static final Map<String, double[]> cache = new HashMap<>();
    private static final ExecutorService executor = Executors.newFixedThreadPool(2);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final OkHttpClient httpClient = new OkHttpClient();

    static {
        // Pré-carrega pontos notórios conhecidos para máxima rapidez e modo offline
        cache.put("av. paulista, 1578", new double[]{-23.5614, -46.6559});
        cache.put("rua augusta, 1200", new double[]{-23.5532, -46.6575});
        cache.put("rua augusta, 500", new double[]{-23.5501, -46.6508});
        cache.put("rua da consolação, 2400", new double[]{-23.5578, -46.6660});
        cache.put("al. santos, 980", new double[]{-23.5670, -46.6530});
        cache.put("av. brigadeiro faria lima, 3477", new double[]{-23.5855, -46.6811});
        cache.put("rua funchal, 418", new double[]{-23.5930, -46.6890});
        cache.put("av. das nações unidas, 12555", new double[]{-23.6080, -46.6970});
        cache.put("av. das nações unidas, 1000", new double[]{-23.5505, -46.6333});
        cache.put("rua oscar freire, 850", new double[]{-23.5645, -46.6710});
        cache.put("rua teodoro sampaio, 1400", new double[]{-23.5590, -46.6800});
        cache.put("rua haddock lobo, 595", new double[]{-23.5580, -46.6620});
        cache.put("rua bela cintra, 1100", new double[]{-23.5550, -46.6600});
    }

    /**
     * Resolve a latitude e longitude real a partir do texto do endereço da entrega.
     */
    public static void geocodeAddress(Context context, String address, GeocodeCallback callback) {
        if (TextUtils.isEmpty(address)) {
            callback.onError("Endereço vazio");
            return;
        }

        String normalizedKey = address.toLowerCase(Locale.ROOT).trim();
        for (Map.Entry<String, double[]> entry : cache.entrySet()) {
            if (normalizedKey.contains(entry.getKey())) {
                callback.onGeocoded(entry.getValue()[0], entry.getValue()[1]);
                return;
            }
        }

        executor.execute(() -> {
            // 1ª Tentativa: Geocoder nativo do Android
            try {
                if (Geocoder.isPresent()) {
                    Geocoder geocoder = new Geocoder(context, new Locale("pt", "BR"));
                    String searchAddress = address;
                    if (!searchAddress.toLowerCase().contains("são paulo") && !searchAddress.toLowerCase().contains("sp")) {
                        searchAddress += ", São Paulo, SP, Brasil";
                    }

                    List<Address> results = geocoder.getFromLocationName(searchAddress, 1);
                    if (results != null && !results.isEmpty()) {
                        double lat = results.get(0).getLatitude();
                        double lon = results.get(0).getLongitude();
                        cache.put(normalizedKey, new double[]{lat, lon});
                        mainHandler.post(() -> callback.onGeocoded(lat, lon));
                        return;
                    }
                }
            } catch (Exception ignored) {
            }

            // 2ª Tentativa: Nominatim OpenStreetMap (serviço público de geocodificação)
            try {
                String encoded = URLEncoder.encode(address + ", São Paulo", StandardCharsets.UTF_8.name());
                String url = "https://nominatim.openstreetmap.org/search?format=json&q=" + encoded + "&limit=1";
                Request req = new Request.Builder()
                        .url(url)
                        .header("User-Agent", "OrbiApp-Android/1.0")
                        .build();

                httpClient.newCall(req).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        mainHandler.post(() -> fallbackToDefaultOrbiPoint(callback));
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        if (response.isSuccessful() && response.body() != null) {
                            try {
                                String body = response.body().string();
                                JSONArray array = new JSONArray(body);
                                if (array.length() > 0) {
                                    JSONObject first = array.getJSONObject(0);
                                    double lat = first.getDouble("lat");
                                    double lon = first.getDouble("lon");
                                    cache.put(normalizedKey, new double[]{lat, lon});
                                    mainHandler.post(() -> callback.onGeocoded(lat, lon));
                                    return;
                                }
                            } catch (Exception ignored) {
                            }
                        }
                        mainHandler.post(() -> fallbackToDefaultOrbiPoint(callback));
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> fallbackToDefaultOrbiPoint(callback));
            }
        });
    }

    private static void fallbackToDefaultOrbiPoint(GeocodeCallback callback) {
        // Ponto central padrão de São Paulo (Praça da Sé)
        callback.onGeocoded(-23.550520, -46.633308);
    }
}
