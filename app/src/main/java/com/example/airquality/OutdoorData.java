package com.example.airquality;

import org.json.JSONException;
import org.json.JSONObject;

public class OutdoorData {
    public float temp = Float.NaN, humidity = Float.NaN, wind = Float.NaN;
    public float pm25 = Float.NaN, pm10 = Float.NaN, no2 = Float.NaN, so2 = Float.NaN, o3 = Float.NaN, co = Float.NaN;
    public int usAqi = -1;
    public long fetchedAt;
    public boolean cached;

    public String toJson() {
        try {
            JSONObject o = new JSONObject();
            put(o, "temp", temp); put(o, "hum", humidity); put(o, "wind", wind);
            put(o, "pm25", pm25); put(o, "pm10", pm10); put(o, "no2", no2); put(o, "so2", so2);
            put(o, "o3", o3); put(o, "co", co);
            o.put("aqi", usAqi);
            o.put("t", fetchedAt);
            return o.toString();
        } catch (JSONException e) {
            return "";
        }
    }

    public static OutdoorData fromJson(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            JSONObject o = new JSONObject(s);
            OutdoorData d = new OutdoorData();
            d.temp = (float) o.optDouble("temp", Double.NaN);
            d.humidity = (float) o.optDouble("hum", Double.NaN);
            d.wind = (float) o.optDouble("wind", Double.NaN);
            d.pm25 = (float) o.optDouble("pm25", Double.NaN);
            d.pm10 = (float) o.optDouble("pm10", Double.NaN);
            d.no2 = (float) o.optDouble("no2", Double.NaN);
            d.so2 = (float) o.optDouble("so2", Double.NaN);
            d.o3 = (float) o.optDouble("o3", Double.NaN);
            d.co = (float) o.optDouble("co", Double.NaN);
            d.usAqi = o.optInt("aqi", -1);
            d.fetchedAt = o.optLong("t", 0);
            return d;
        } catch (JSONException e) {
            return null;
        }
    }

    private static void put(JSONObject o, String k, float v) throws JSONException {
        if (!Float.isNaN(v)) o.put(k, (double) v);
    }
}
