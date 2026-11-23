package com.example.airquality;

import android.content.Context;
import android.content.SharedPreferences;

public class AppPrefs {
    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("aq_prefs", Context.MODE_PRIVATE);
    }
    public static String getToken(Context c)  { return sp(c).getString("token", ""); }
    public static String getServer(Context c) { return sp(c).getString("server", "blynk.cloud"); }
    public static String getLat(Context c)    { return sp(c).getString("lat", ""); }
    public static String getLon(Context c)    { return sp(c).getString("lon", ""); }
    public static String getPlace(Context c)  { return sp(c).getString("place", ""); }
    public static String getOutdoorCache(Context c) { return sp(c).getString("outdoor_cache", ""); }

    public static void save(Context c, String token, String server) {
        sp(c).edit().putString("token", token).putString("server", server).apply();
    }
    public static void saveLocation(Context c, String lat, String lon, String place) {
        sp(c).edit().putString("lat", lat).putString("lon", lon).putString("place", place).apply();
    }
    public static void setOutdoorCache(Context c, String json) {
        sp(c).edit().putString("outdoor_cache", json).apply();
    }
}
