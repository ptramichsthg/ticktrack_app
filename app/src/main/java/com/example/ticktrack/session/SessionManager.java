package com.example.ticktrack.session;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private static final String PREF_NAME = "TickTrackSession";
    
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_NAME = "name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_ROLE = "role"; // "admin" or "user"

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void saveLoginSession(String token, int userId, String name, String email, String role) {
        editor.putString(KEY_TOKEN, token);
        editor.putInt(KEY_USER_ID, userId);
        editor.putString(KEY_NAME, name);
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_ROLE, role);
        editor.commit();
    }

    public void updateProfile(String name, String email) {
        editor.putString(KEY_NAME, name);
        editor.putString(KEY_EMAIL, email);
        editor.commit();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public String getRole() {
        return prefs.getString(KEY_ROLE, "user");
    }

    public boolean isLoggedIn() {
        return getToken() != null;
    }

    public int getUserId() {
        return prefs.getInt(KEY_USER_ID, -1);
    }

    public String getName() {
        return prefs.getString(KEY_NAME, "User");
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }

    public void setDarkMode(boolean isDark) {
        editor.putBoolean("dark_mode", isDark);
        editor.apply();
    }

    public boolean isDarkMode() {
        return prefs.getBoolean("dark_mode", false);
    }

    public void setNotificationEnabled(boolean value) {
        editor.putBoolean("notification_enabled", value);
        editor.apply();
    }

    public boolean isNotificationEnabled() {
        return prefs.getBoolean("notification_enabled", true);
    }

    public void logout() {
        editor.clear();
        editor.apply();
    }
}
