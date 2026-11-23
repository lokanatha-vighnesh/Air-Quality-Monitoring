package com.example.airquality;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "readings", indices = {@Index(value = {"source", "ts"})})
public class ReadingEntity {
    @PrimaryKey(autoGenerate = true) public long id;
    public long ts;
    public float gas;
    @NonNull public String source = "sim";     // "live" (Blynk) or "sim"

    public ReadingEntity() { }

    @Ignore
    public ReadingEntity(long ts, float gas, @NonNull String source) {
        this.ts = ts;
        this.gas = gas;
        this.source = source;
    }

    public Reading toReading() { return new Reading(ts, gas); }
}