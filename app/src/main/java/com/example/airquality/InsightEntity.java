package com.example.airquality;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "insights", indices = {@Index(value = {"source", "ts"})})
public class InsightEntity {
    @PrimaryKey(autoGenerate = true) public long id;
    public long ts;
    @NonNull public String kind = "alert";        // "alert" or "recommendation"
    @NonNull public String title = "";
    @NonNull public String message = "";
    public int level;                             // Level ordinal
    @NonNull public String source = "sim";

    public InsightEntity() { }

    @Ignore
    public InsightEntity(long ts, @NonNull String kind, @NonNull String title, @NonNull String message,
                         int level, @NonNull String source) {
        this.ts = ts; this.kind = kind; this.title = title; this.message = message;
        this.level = level; this.source = source;
    }
}