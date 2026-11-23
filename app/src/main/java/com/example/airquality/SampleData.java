package com.example.airquality;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Simulated gas readings (demo / testing without hardware). */
public class SampleData {
    private static final long STEP_MS = 30 * 60 * 1000L;
    private static final Random rnd = new Random(42);

    private static double sq(double x) { return x * x; }

    /** 24 h of history with two "cooking" bumps. */
    public static List<Reading> history(int points) {
        long now = System.currentTimeMillis();
        List<Reading> list = new ArrayList<>();
        for (int i = 0; i < points; i++) {
            double t = i / (double) points;
            double g = 6 + 4 * Math.sin(2 * Math.PI * t * 2)
                    + 220 * Math.exp(-sq((t - 0.32) / 0.03))
                    + 90 * Math.exp(-sq((t - 0.78) / 0.04))
                    + rnd.nextFloat() * 2;
            list.add(new Reading(now - (points - 1 - i) * STEP_MS, (float) Math.max(g, 1.0)));
        }
        return list;
    }

    /** Mean-reverting random walk with occasional spikes. */
    public static Reading next(Reading p) {
        double g = p.gas + (10 - p.gas) * 0.05 + (rnd.nextFloat() - 0.5f) * 4;
        if (rnd.nextFloat() < 0.01f) g += 300;
        return new Reading(System.currentTimeMillis(), (float) Math.min(Math.max(g, 1), 3000));
    }
}