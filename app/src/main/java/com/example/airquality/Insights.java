package com.example.airquality;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Rule-based, context-aware recommendations (indoor gas + forecast + outdoor air). General advice only. */
public class Insights {

    /** Outdoor level from US AQI (or PM2.5 if AQI missing); null when unknown. */
    public static Level outdoorLevel(OutdoorData o) {
        if (o == null) return null;
        if (o.usAqi >= 0) {
            if (o.usAqi <= 50) return Level.GOOD;
            if (o.usAqi <= 100) return Level.MODERATE;
            if (o.usAqi <= 150) return Level.POOR;
            return Level.UNHEALTHY;
        }
        if (!Float.isNaN(o.pm25)) {
            if (o.pm25 <= 12f) return Level.GOOD;
            if (o.pm25 <= 35.4f) return Level.MODERATE;
            if (o.pm25 <= 55.4f) return Level.POOR;
            return Level.UNHEALTHY;
        }
        return null;
    }

    public static List<String> build(Reading in, OutdoorData out, Forecaster.Forecast fc) {
        List<String> t = new ArrayList<>();
        Level lv = in.overall();
        Level ol = outdoorLevel(out);
        String aqi = (out != null && out.usAqi >= 0) ? " (US AQI " + out.usAqi + ")" : "";

        if (lv == Level.UNHEALTHY) {
            t.add("Gas/smoke reading is very high. Ventilate now, avoid open flames and electrical switches, "
                    + "and leave the area if you smell gas.");
        } else if (lv == Level.POOR) {
            t.add("Gas/smoke reading is elevated. Ventilate and check for sources such as cooking, burning or aerosol sprays.");
        } else if (lv == Level.MODERATE) {
            t.add("Gas/smoke level is slightly raised. Cooking or cleaning products may be the cause; a little ventilation will help.");
        }
        if (fc != null && fc.error == null && fc.level.ordinal() > lv.ordinal()
                && fc.level.ordinal() >= Level.MODERATE.ordinal()) {
            t.add(String.format(Locale.US, "Level is forecast to rise to about %s ppm-eq (%s) within the hour.",
                    Param.GAS.format(fc.gas), fc.level.label));
        }
        if (ol != null) {
            if (lv.ordinal() >= Level.MODERATE.ordinal()) {
                if (ol.ordinal() >= Level.POOR.ordinal()) {
                    t.add("Outdoor air is also poor" + aqi + ". Ventilate only briefly or use filtered ventilation.");
                } else {
                    t.add("Outdoor air is better" + aqi + ". Opening windows should help.");
                }
            } else if (ol.ordinal() >= Level.POOR.ordinal()) {
                t.add("Outdoor air quality is poor" + aqi + ". Keep windows closed and consider an air purifier.");
            }
        }
        if (t.isEmpty()) t.add("Indoor gas level is normal. No action needed.");
        return t.size() > 4 ? t.subList(0, 4) : t;
    }
}