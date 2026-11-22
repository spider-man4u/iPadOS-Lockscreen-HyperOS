package com.ipados.lockscreen;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsManager {
    private static final String PREF_NAME = "ipados_lockscreen_prefs";
    private static final String KEY_CLOCK_COLOR = "clock_color";
    private static final String KEY_CLOCK_FONT = "clock_font";
    private static final String KEY_DEPTH_ENABLED = "depth_enabled";
    private static final String KEY_WALLPAPER_TYPE = "wallpaper_type";
    private static final String KEY_CUSTOM_WALLPAPER_URI = "custom_wallpaper_uri";
    private static final String KEY_FILTER_INDEX = "filter_index";

    public static final int DEFAULT_COLOR = 0xFFA3E7FC; // Apple iPad Cyan
    public static final String FONT_SF_ROUNDED = "fonts/SF-Pro-Rounded-Bold.ttf";

    private final SharedPreferences prefs;

    public SettingsManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public int getClockColor() {
        return prefs.getInt(KEY_CLOCK_COLOR, DEFAULT_COLOR);
    }

    public void setClockColor(int color) {
        prefs.edit().putInt(KEY_CLOCK_COLOR, color).apply();
    }

    public String getClockFont() {
        return prefs.getString(KEY_CLOCK_FONT, FONT_SF_ROUNDED);
    }

    public void setClockFont(String font) {
        prefs.edit().putString(KEY_CLOCK_FONT, font).apply();
    }

    public boolean isDepthEnabled() {
        return prefs.getBoolean(KEY_DEPTH_ENABLED, true);
    }

    public void setDepthEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_DEPTH_ENABLED, enabled).apply();
    }

    public String getWallpaperType() {
        return prefs.getString(KEY_WALLPAPER_TYPE, "ipad_clean.png");
    }

    public void setWallpaperType(String type) {
        prefs.edit().putString(KEY_WALLPAPER_TYPE, type).apply();
    }

    public String getCustomWallpaperUri() {
        return prefs.getString(KEY_CUSTOM_WALLPAPER_URI, "");
    }

    public void setCustomWallpaperUri(String uri) {
        prefs.edit().putString(KEY_CUSTOM_WALLPAPER_URI, uri).apply();
    }

    public int getFilterIndex() {
        return prefs.getInt(KEY_FILTER_INDEX, 0);
    }

    public void setFilterIndex(int index) {
        prefs.edit().putInt(KEY_FILTER_INDEX, index).apply();
    }
}
