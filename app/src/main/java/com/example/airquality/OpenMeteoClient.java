package com.example.airquality;

import org.json.JSONObject;

/** Weather + air quality from Open-Meteo (free, no API key). Call from a background thread. */
public class OpenMeteoClient {

    public static class Result {
        public OutdoorData data;
        public String error;
    }

    public static Result fetch(double lat, double lon) {
        Result r = new Result();
        try {
            String loc = "latitude=" + lat + "&longitude=" + lon;
            String w = getWithRetry("https://api.open-meteo.com/v1/forecast?" + loc
                    + "&current=temperature_2m,relative_humidity_2m,wind_speed_10m");
            String a = getWithRetry("https://air-quality-api.open-meteo.com/v1/air-quality?" + loc
                    + "&current=pm2_5,pm10,carbon_monoxide,nitrogen_dioxide,sulphur_dioxide,ozone,us_aqi");

            JSONObject wc = new JSONObject(w).getJSONObject("current");
            JSONObject ac = new JSONObject(a).getJSONObject("current");

            OutdoorData d = new OutdoorData();
            d.temp = (float) wc.optDouble("temperature_2m", Double.NaN);
            d.humidity = (float) wc.optDouble("relative_humidity_2m", Double.NaN);
            d.wind = (float) wc.optDouble("wind_speed_10m", Double.NaN);
            d.pm25 = (float) ac.optDouble("pm2_5", Double.NaN);
            d.pm10 = (float) ac.optDouble("pm10", Double.NaN);
            d.co = (float) ac.optDouble("carbon_monoxide", Double.NaN);
            d.no2 = (float) ac.optDouble("nitrogen_dioxide", Double.NaN);
            d.so2 = (float) ac.optDouble("sulphur_dioxide", Double.NaN);
            d.o3 = (float) ac.optDouble("ozone", Double.NaN);
            d.usAqi = ac.optInt("us_aqi", -1);
            d.fetchedAt = System.currentTimeMillis();

            if (Float.isNaN(d.pm25) && Float.isNaN(d.temp)) {
                r.error = "API returned no values for this location";
                return r;
            }
            r.data = d;
        } catch (Exception e) {
            r.error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        }
        return r;
    }

    /** Up to 3 attempts with growing delay; client errors (HTTP 4xx) are not retried. */
    private static String getWithRetry(String url) throws Exception {
        Exception last = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                return Http.get(url);
            } catch (Exception e) {
                last = e;
                String m = e.getMessage();
                if (m != null && m.startsWith("HTTP 4")) break;
                try { Thread.sleep(1500L * attempt); } catch (InterruptedException ie) { break; }
            }
        }
        throw last;
    }
}
