package com.example.airquality;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;

/** Reads the MQ-2 gas index (V0) from Blynk Cloud. Call from a background thread. */
public class BlynkClient {

    public static class Result {
        public Reading reading;
        public boolean deviceOnline;
        public String error;
    }

    public static Result fetch(String server, String token) {
        Result r = new Result();
        try {
            String host = server.trim().replace("https://", "").replace("http://", "");
            while (host.endsWith("/")) host = host.substring(0, host.length() - 1);
            if (host.isEmpty()) host = "blynk.cloud";
            String t = URLEncoder.encode(token.trim(), "UTF-8");
            String base = "https://" + host + "/external/api/";

            r.deviceOnline = Http.get(base + "isHardwareConnected?token=" + t).trim().equalsIgnoreCase("true");
            double gas = parseGas(Http.get(base + "get?token=" + t + "&v0"));
            if (Double.isNaN(gas)) {
                r.error = "No data yet on V0 (is the ESP32 sending?)";
                return r;
            }
            r.reading = new Reading(System.currentTimeMillis(), (float) gas);
        } catch (Exception e) {
            r.error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        }
        return r;
    }

    /** Blynk returns a plain number for one pin; also tolerate {"v0":..} and ["..."]. */
    static double parseGas(String body) throws JSONException {
        String s = body.trim();
        try {
            if (s.startsWith("{")) return new JSONObject(s).optDouble("v0", Double.NaN);
            if (s.startsWith("[")) return new JSONArray(s).optDouble(0, Double.NaN);
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }
}