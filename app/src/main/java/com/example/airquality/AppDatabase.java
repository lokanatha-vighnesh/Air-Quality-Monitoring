package com.example.airquality;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {ReadingEntity.class, InsightEntity.class, PredictionEntity.class},
        version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract AqDao dao();

    /** One thread for every database job, so writes and reads are naturally serialized. */
    public static final ExecutorService IO = Executors.newSingleThreadExecutor();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase get(Context c) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(c.getApplicationContext(), AppDatabase.class, "airquality.db").build();
                }
            }
        }
        return INSTANCE;
    }
}