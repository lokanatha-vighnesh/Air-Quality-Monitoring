package com.example.airquality;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "predictions", indices = {@Index(value = {"source", "ts"})})
public class PredictionEntity {
    @PrimaryKey(autoGenerate = true) public long id;
    public long ts;              // when the forecast was made
    public float predicted;      // forecast for ts + 1 h
    public float current;        // value at ts (persistence baseline)
    @NonNull public String source = "sim";

    public PredictionEntity() { }

    @Ignore
    public PredictionEntity(long ts, float predicted, float current, @NonNull String source) {
        this.ts = ts; this.predicted = predicted; this.current = current; this.source = source;
    }
}