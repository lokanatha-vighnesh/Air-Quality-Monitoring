package com.example.airquality;

public class AlertItem {
    public final String title, message, timeLabel;
    public final Level level;

    public AlertItem(String title, String message, Level level, String timeLabel) {
        this.title = title;
        this.message = message;
        this.level = level;
        this.timeLabel = timeLabel;
    }
}