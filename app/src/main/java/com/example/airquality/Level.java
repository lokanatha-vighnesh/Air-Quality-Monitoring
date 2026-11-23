package com.example.airquality;

public enum Level {
    GOOD("Good", 0xFF2E9E5B),
    MODERATE("Moderate", 0xFFE0A800),
    POOR("Poor", 0xFFEF8A17),
    UNHEALTHY("Unhealthy", 0xFFD64545);

    public final String label;
    public final int color;

    Level(String label, int color) {
        this.label = label;
        this.color = color;
    }
}