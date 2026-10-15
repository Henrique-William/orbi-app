package com.orbi.orbi_app.data;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "orbi_session_prefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_DRIVER_NAME = "driver_name";
    private static final String KEY_DRIVER_EMAIL = "driver_email";
    private static final String KEY_SERVER_URL = "server_url";

    public static final String DEFAULT_SERVER_URL = "http://10.0.2.2:8080/";

    private final SharedPreferences preferences;

    public SessionManager(Context context) {
        this.preferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveSession(String email, String token, String driverName) {
        preferences.edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putString(KEY_DRIVER_EMAIL, email)
                .putString(KEY_TOKEN, token)
                .putString(KEY_DRIVER_NAME, driverName)
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

    public String getAuthToken() {
        return preferences.getString(KEY_TOKEN, "");
    }

    public String getServerUrl() {
        return preferences.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL);
    }

    public void setServerUrl(String url) {
        if (!url.endsWith("/")) {
            url += "/";
        }
        preferences.edit().putString(KEY_SERVER_URL, url).apply();
    }

    public void clearSession() {
        preferences.edit().clear().apply();
    }
}
