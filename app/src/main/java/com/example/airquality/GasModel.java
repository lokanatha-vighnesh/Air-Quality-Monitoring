package com.example.airquality;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Locale;

/** Runs the exported scikit-learn model (ridge or tree ensemble) from assets/aq_model.json. */
public class GasModel implements Forecaster.Predictor {
    public final String type;               // "ridge" or "gbr"
    public final float relMae, relMaePers;  // offline test error, % of mean level

    private double[] mean, scale, coef;
    private double intercept, init, lr;
    private Tree[] trees;

    private static class Tree {
        int[] left, right, feature;
        double[] thr, val;
    }

    public static GasModel load(Context c) throws Exception {
        InputStream in = c.getAssets().open("aq_model.json");
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
        in.close();
        return fromJson(bo.toString("UTF-8"));
    }

    public static GasModel fromJson(String json) throws JSONException {
        return new GasModel(new JSONObject(json));
    }

    private GasModel(JSONObject o) throws JSONException {
        type = o.getString("type");
        relMae = (float) o.optDouble("test_mae_rel", Double.NaN);
        relMaePers = (float) o.optDouble("test_mae_rel_persistence", Double.NaN);
        if (o.getJSONArray("features").length() != Forecaster.NUM_FEATURES) {
            throw new JSONException("feature count mismatch; retrain with the MQ-2 train_model.py");
        }
        if (type.equals("ridge")) {
            mean = d(o.getJSONArray("mean"));
            scale = d(o.getJSONArray("scale"));
            coef = d(o.getJSONArray("coef"));
            intercept = o.getDouble("intercept");
        } else {
            init = o.getDouble("init");
            lr = o.getDouble("lr");
            JSONArray ta = o.getJSONArray("trees");
            trees = new Tree[ta.length()];
            for (int i = 0; i < trees.length; i++) {
                JSONObject t = ta.getJSONObject(i);
                Tree tr = new Tree();
                tr.left = iarr(t.getJSONArray("left"));
                tr.right = iarr(t.getJSONArray("right"));
                tr.feature = iarr(t.getJSONArray("feature"));
                tr.thr = d(t.getJSONArray("threshold"));
                tr.val = d(t.getJSONArray("value"));
                trees[i] = tr;
            }
        }
    }

    @Override public float predict(float[] x) {
        if (type.equals("ridge")) {
            double s = intercept;
            for (int i = 0; i < coef.length; i++) s += ((x[i] - mean[i]) / scale[i]) * coef[i];
            return (float) s;
        }
        double s = init;
        for (Tree t : trees) {
            int n = 0;
            while (t.left[n] != -1) n = x[t.feature[n]] <= t.thr[n] ? t.left[n] : t.right[n];
            s += lr * t.val[n];
        }
        return (float) s;
    }

    public String describe() {
        return String.format(Locale.US,
                "Model: %s · offline test error %.1f%% of level (persistence %.1f%%) · UCI baseline",
                type, relMae, relMaePers);
    }

    private static double[] d(JSONArray a) throws JSONException {
        double[] r = new double[a.length()];
        for (int i = 0; i < r.length; i++) r[i] = a.getDouble(i);
        return r;
    }

    private static int[] iarr(JSONArray a) throws JSONException {
        int[] r = new int[a.length()];
        for (int i = 0; i < r.length; i++) r[i] = a.getInt(i);
        return r;
    }
}