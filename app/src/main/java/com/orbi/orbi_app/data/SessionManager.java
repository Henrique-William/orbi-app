package com.orbi.orbi_app.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Base64;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public class SessionManager {
    private static final String PREF_NAME = "orbi_session_prefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_DRIVER_NAME = "driver_name";
    private static final String KEY_DRIVER_EMAIL = "driver_email";
    private static final String KEY_DRIVER_USER_ID = "driver_user_id";
    private static final String KEY_SERVER_URL = "server_url";

    public static final String DEFAULT_EMULATOR_URL = "http://10.0.2.2:8080/";
    public static final String DEFAULT_LAN_URL = "http://192.168.1.60:8080/";
    public static final String DEFAULT_SERVER_URL = DEFAULT_EMULATOR_URL;

    private final SharedPreferences preferences;

    public SessionManager(Context context) {
        this.preferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveSession(String email, String token, String driverName) {
        String userId = extractUserIdFromJwt(token);
        preferences.edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putString(KEY_DRIVER_EMAIL, email)
                .putString(KEY_TOKEN, token)
                .putString(KEY_DRIVER_NAME, driverName)
                .putString(KEY_DRIVER_USER_ID, userId)
                .apply();
    }

    public void saveSession(String email, String token, String driverName, String driverUserId) {
        preferences.edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putString(KEY_DRIVER_EMAIL, email)
                .putString(KEY_TOKEN, token)
                .putString(KEY_DRIVER_NAME, driverName)
                .putString(KEY_DRIVER_USER_ID, driverUserId != null ? driverUserId : "")
                .apply();
    }

    public boolean isLoggedIn() {
        return preferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getDriverName() {
        return preferences.getString(KEY_DRIVER_NAME, "Motorista");
    }

    public String getDriverEmail() {
        return preferences.getString(KEY_DRIVER_EMAIL, "");
    }

    public String getDriverUserId() {
        return preferences.getString(KEY_DRIVER_USER_ID, "");
    }

    public String getAuthToken() {
        return preferences.getString(KEY_TOKEN, "");
    }

    public String getServerUrl() {
        String defaultUrl = isEmulator() ? DEFAULT_EMULATOR_URL : DEFAULT_LAN_URL;
        return preferences.getString(KEY_SERVER_URL, defaultUrl);
    }

    public void setServerUrl(String url) {
        if (url == null) return;
        url = url.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }
        if (!url.endsWith("/")) {
            url += "/";
        }
        preferences.edit().putString(KEY_SERVER_URL, url).apply();
    }

    public static boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT)
                || Build.PRODUCT.contains("sdk_gphone")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu");
    }

    public void clearSession() {
        preferences.edit().clear().apply();
    }

    public static String extractUserIdFromJwt(String token) {
        if (token == null || !token.contains(".")) {
            return "";
        }
        try {
            String[] parts = token.split("\\.");
            if (parts.length >= 2) {
                byte[] decodedBytes = Base64.decode(parts[1], Base64.URL_SAFE);
                String payload = new String(decodedBytes, StandardCharsets.UTF_8);
                JSONObject json = new JSONObject(payload);
                return json.optString("sub", "");
            }
        } catch (Exception ignored) {
        }
        return "";
    }
}
