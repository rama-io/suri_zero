package com.rama.suri_zero.managers;

import android.content.Context;
import android.content.SharedPreferences;

import com.rama.suri_zero.objects.Action;
import com.rama.suri_zero.objects.Gesture;
import com.rama.suri_zero.objects.PrefTheme;

public class PrefsManager {
    private static final String PREFS_NAME = "suri_zero";
    private static final String KEY_THEME = "settings:theme";
    private static final String KEY_THEME_ROLLED_ID = "settings:theme_rolled_id";
    private static final String KEY_ZOOM_PERCENT = "settings:zoom_percent";
    private static PrefsManager instance;
    private final SharedPreferences prefs;

    private PrefsManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized PrefsManager getInstance(Context context) {
        if (instance == null) {
            instance = new PrefsManager(context);
        }
        return instance;
    }

    public String getTheme() {
        return prefs.getString(KEY_THEME, PrefTheme.DEFAULT);
    }

    public void setTheme(String themeId) {
        prefs.edit().putString(KEY_THEME, themeId).commit();
    }

    public String getRolledThemeId() {
        return prefs.getString(KEY_THEME_ROLLED_ID, PrefTheme.CATPPUCCIN_MOCHA_MAUVE);
    }

    public void setRolledTheme(String themeId) {
        prefs.edit().putString(KEY_THEME_ROLLED_ID, themeId).commit();
    }

    public int getZoomPercent() {
        return prefs.getInt(KEY_ZOOM_PERCENT, 100);
    }

    public void setZoomPercent(int percent) {
        prefs.edit().putInt(KEY_ZOOM_PERCENT, percent).commit();
    }

    private String gestureKey(Gesture gesture, String field) {
        return "gesture:" + gesture.id + ":" + field;
    }

    public Action getGestureAction(Gesture gesture) {
        String id = prefs.getString(gestureKey(gesture, "action"), null);
        return id == null ? gesture.defaultAction : Action.fromId(id);
    }

    public int getGestureAmount(Gesture gesture, Action action) {
        return prefs.getInt(gestureKey(gesture, "amount"), action.defaultAmount);
    }

    public String getGestureApp(Gesture gesture) {
        return prefs.getString(gestureKey(gesture, "app"), null);
    }

    public void setGesture(Gesture gesture, Action action, int amount, String appPackage) {
        prefs.edit()
                .putString(gestureKey(gesture, "action"), action.id)
                .putInt(gestureKey(gesture, "amount"), amount)
                .putString(gestureKey(gesture, "app"), appPackage)
                .commit();
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return prefs.getBoolean(key, defaultValue);
    }

    public void setBoolean(String key, boolean value) {
        prefs.edit().putBoolean(key, value).commit();
    }
}
