package com.example.airquality;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Builds model features from hourly gas averages and produces a next-hour forecast. */
public class Forecaster {
    public static final int NUM_FEATURES = 7;
    public static final double EPS = 0.1;          // must match train_model.py
    private static final long H = 3600000L;

    public interface Predictor {
        /** Returns the predicted change in log-space: ln(next+EPS) - ln(current+EPS). */
        float predict(float[] x);
    }

    public static class Forecast {
        public float gas;
        public Level level;
        public int found;              // how many of the 5 needed hourly values exist (max 5)
        public boolean lowConfidence;
        public String error;           // non-null when no forecast is available
        public String modelInfo = "";
    }

    /** Average gas value per clock hour (bucket start in epoch ms), oldest first. */
    public static TreeMap<Long, Float> hourly(List<Reading> rows) {
        TreeMap<Long, double[]> acc = new TreeMap<>();
        for (Reading r : rows) {
            long h = r.timestamp / H * H;
            double[] a = acc.get(h);
            if (a == null) { a = new double[2]; acc.put(h, a); }
            a[0] += r.gas;
            a[1] += 1;
        }
        TreeMap<Long, Float> out = new TreeMap<>();
        for (Map.Entry<Long, double[]> e : acc.entrySet()) {
            out.put(e.getKey(), (float) (e.getValue()[0] / e.getValue()[1]));
        }
        return out;
    }

    public static double hourOfDay(long ms) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ms);
        return c.get(Calendar.HOUR_OF_DAY);
    }

    private static double u(double v) {
        return Math.log(v + EPS);
    }

    /** Value k hours before cur; if missing, nearest older hour within 6 more hours, else the fallback. */
    private static float lag(Map<Long, Float> m, long cur, int k, float fallback) {
        for (int j = k; j <= k + 6; j++) {
            Float v = m.get(cur - j * H);
            if (v != null) return v;
        }
        return fallback;
    }

    /** Feature order MUST match FEATURES in train_model.py. Uses only data older than cur. */
    public static float[] features(Map<Long, Float> hrs, long cur, float curVal) {
        double uc = u(curVal);
        double u1 = u(lag(hrs, cur, 1, curVal));
        double u2 = u(lag(hrs, cur, 2, curVal));
        double u3 = u(lag(hrs, cur, 3, curVal));
        double u6 = u(lag(hrs, cur, 6, curVal));
        double hr = hourOfDay(cur);
        float[] x = new float[NUM_FEATURES];
        x[0] = (float) (u1 - uc);
        x[1] = (float) (u2 - uc);
        x[2] = (float) (u3 - uc);
        x[3] = (float) (u6 - uc);
        x[4] = (float) ((uc + u1 + u2) / 3.0 - uc);
        x[5] = (float) Math.sin(2 * Math.PI * hr / 24.0);
        x[6] = (float) Math.cos(2 * Math.PI * hr / 24.0);
        return x;
    }

    /** Converts the model's log-space change into a gas value. */
    public static float apply(float[] x, float curVal, Predictor p) {
        double next = Math.exp(u(curVal) + p.predict(x)) - EPS;
        return (float) Math.max(0.0, next);
    }

    public static Forecast predict(Predictor m, List<Reading> rows, Reading now) {
        TreeMap<Long, Float> hrs = hourly(rows);
        long cur = now.timestamp / H * H;
        Float avg = hrs.get(cur);
        float curVal = avg != null ? avg : now.gas;
        if (avg == null) hrs.put(cur, now.gas);

        int found = 0;
        for (int k : new int[]{0, 1, 2, 3, 6}) if (hrs.containsKey(cur - k * H)) found++;

        Forecast f = new Forecast();
        f.gas = apply(features(hrs, cur, curVal), curVal, m);
        f.level = Param.GAS.classify(f.gas);
        f.found = found;
        f.lowConfidence = found < 5;
        return f;
    }
}