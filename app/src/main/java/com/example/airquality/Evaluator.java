package com.example.airquality;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Phase 6: backtests next-hour predictions against what was actually measured. */
public class Evaluator {
    private static final long H = 3600000L;

    public static class Result {
        public int n;
        public boolean enough;                         // n >= 5
        public double maeModel = Double.NaN, rmseModel = Double.NaN, f1Model = Double.NaN;
        public double maePers = Double.NaN, rmsePers = Double.NaN, f1Pers = Double.NaN;
    }

    /** m may be null (then only the persistence baseline is computed). */
    public static Result backtest(List<Reading> rows, Forecaster.Predictor m) {
        TreeMap<Long, Float> hrs = Forecaster.hourly(rows);
        List<Float> act = new ArrayList<>(), pm = new ArrayList<>(), pp = new ArrayList<>();
        for (Map.Entry<Long, Float> e : hrs.entrySet()) {
            Float next = hrs.get(e.getKey() + H);
            if (next == null) continue;
            float cur = e.getValue();
            act.add(next);
            pp.add(cur);                                           // persistence: next = current
            if (m != null) pm.add(Forecaster.apply(Forecaster.features(hrs, e.getKey(), cur), cur, m));
        }
        Result r = new Result();
        r.n = act.size();
        r.enough = r.n >= 5;
        if (r.n == 0) return r;
        r.maePers = mae(act, pp);
        r.rmsePers = rmse(act, pp);
        r.f1Pers = f1Macro(act, pp);
        if (m != null) {
            r.maeModel = mae(act, pm);
            r.rmseModel = rmse(act, pm);
            r.f1Model = f1Macro(act, pm);
        }
        return r;
    }

    static double mae(List<Float> a, List<Float> p) {
        double s = 0;
        for (int i = 0; i < a.size(); i++) s += Math.abs(a.get(i) - p.get(i));
        return s / a.size();
    }

    static double rmse(List<Float> a, List<Float> p) {
        double s = 0;
        for (int i = 0; i < a.size(); i++) { double d = a.get(i) - p.get(i); s += d * d; }
        return Math.sqrt(s / a.size());
    }

    /** Macro F1 over the level classes that occur in actual or predicted values. */
    static double f1Macro(List<Float> a, List<Float> p) {
        int k = Level.values().length;
        int[] tp = new int[k], fp = new int[k], fn = new int[k];
        for (int i = 0; i < a.size(); i++) {
            int ya = Param.GAS.classify(a.get(i)).ordinal();
            int yp = Param.GAS.classify(p.get(i)).ordinal();
            if (ya == yp) tp[ya]++; else { fn[ya]++; fp[yp]++; }
        }
        double sum = 0;
        int used = 0;
        for (int c = 0; c < k; c++) {
            if (tp[c] + fp[c] + fn[c] == 0) continue;
            sum += 2.0 * tp[c] / (2.0 * tp[c] + fp[c] + fn[c]);
            used++;
        }
        return used == 0 ? Double.NaN : sum / used;
    }
}