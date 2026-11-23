package com.example.airquality;

import java.util.Locale;

/** Monitored parameters. With an MQ-2 only, there is one indoor channel: a gas / smoke index. */
public enum Param {
    GAS("Gas / Smoke (MQ-2)", "ppm-eq", "MQ-2 analog gas sensor on ESP32",
            "0 – 50 ppm-eq (clean air)");

    public static final float GOOD_MAX = 50f, MODERATE_MAX = 200f, POOR_MAX = 1000f;

    public final String label, unit, sensor, idealRange;

    Param(String label, String unit, String sensor, String idealRange) {
        this.label = label;
        this.unit = unit;
        this.sensor = sensor;
        this.idealRange = idealRange;
    }

    public Level classify(float v) {
        if (v <= GOOD_MAX) return Level.GOOD;
        if (v <= MODERATE_MAX) return Level.MODERATE;
        if (v <= POOR_MAX) return Level.POOR;
        return Level.UNHEALTHY;
    }
    /** Position of a gas value on the 0..1 gauge arc: each level band takes a quarter of the arc. */
    public static float gaugeFraction(float g) {
        if (!(g > 0f)) return 0f;
        if (g <= GOOD_MAX) return 0.25f * g / GOOD_MAX;
        if (g <= MODERATE_MAX) return 0.25f + 0.25f * (g - GOOD_MAX) / (MODERATE_MAX - GOOD_MAX);
        if (g <= POOR_MAX) return 0.5f + 0.25f * (g - MODERATE_MAX) / (POOR_MAX - MODERATE_MAX);
        return 0.75f + 0.25f * Math.min(1f, (g - POOR_MAX) / 1000f);
    }
    public String format(float v) {
        return v < 10f ? String.format(Locale.US, "%.1f", v) : String.format(Locale.US, "%.0f", v);
    }
}