package com.example.airquality;

import java.util.ArrayList;
import java.util.List;

/** Pure rules that decide which alerts a reading (and forecast) should raise. */
public class AlertRules {

    public static class Alert {
        public final String key, title, message;
        public final Level level;
        Alert(String key, String title, String message, Level level) {
            this.key = key; this.title = title; this.message = message; this.level = level;
        }
    }

    public static List<Alert> evaluate(Reading r, Forecaster.Forecast fc) {
        List<Alert> out = new ArrayList<>();
        Level lv = r.overall();
        String v = Param.GAS.format(r.gas);
        if (lv == Level.UNHEALTHY) {
            out.add(new Alert("gas_unhealthy", "Very high gas / smoke level",
                    "Gas index " + v + " ppm-eq. Ventilate, avoid open flames and switches, and leave the area "
                            + "if you smell gas. The MQ-2 is an indicator, not a certified gas detector.",
                    Level.UNHEALTHY));
        } else if (lv == Level.POOR) {
            out.add(new Alert("gas_poor", "Elevated gas / smoke level",
                    "Gas index " + v + " ppm-eq. Ventilate and look for sources such as cooking, burning or sprays.",
                    Level.POOR));
        }
        if (fc != null && fc.error == null && fc.level.ordinal() >= Level.POOR.ordinal()
                && lv.ordinal() < Level.POOR.ordinal()) {
            out.add(new Alert("gas_forecast", "Gas level forecast to rise",
                    "Next-hour forecast: about " + Param.GAS.format(fc.gas) + " ppm-eq (" + fc.level.label
                            + "). Consider ventilating before it rises.", fc.level));
        }
        return out;
    }
}