package com.example.airquality;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface AqDao {
    @Insert void insertReading(ReadingEntity r);
    @Insert void insertReadings(List<ReadingEntity> list);
    @Query("SELECT * FROM readings WHERE source = :src AND ts >= :from ORDER BY ts ASC")
    List<ReadingEntity> readingsSince(String src, long from);
    @Query("SELECT * FROM readings WHERE source = :src ORDER BY ts DESC LIMIT :n")
    List<ReadingEntity> lastReadings(String src, int n);
    @Query("DELETE FROM readings WHERE source = :src") void deleteReadings(String src);
    @Query("DELETE FROM readings WHERE ts < :before") void pruneReadings(long before);

    @Insert void insertInsight(InsightEntity e);
    @Query("SELECT * FROM insights WHERE source = :src ORDER BY ts DESC LIMIT :n")
    List<InsightEntity> insights(String src, int n);
    @Query("SELECT COUNT(*) FROM insights WHERE source = :src") int countInsights(String src);
    @Query("DELETE FROM insights WHERE source = :src") void deleteInsights(String src);

    @Insert void insertPrediction(PredictionEntity p);
    @Query("SELECT * FROM predictions WHERE source = :src ORDER BY ts DESC LIMIT :n")
    List<PredictionEntity> predictions(String src, int n);
    @Query("SELECT COUNT(*) FROM predictions WHERE source = :src") int countPredictions(String src);
    @Query("DELETE FROM predictions WHERE source = :src") void deletePredictions(String src);
}