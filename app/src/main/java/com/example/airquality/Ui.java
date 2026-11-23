package com.example.airquality;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;

import androidx.core.graphics.ColorUtils;

public class Ui {
    public static int dp(Context c, int v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    /** Rounded "pill" background with a translucent tint of the given colour. */
    public static GradientDrawable pill(Context c, int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(ColorUtils.setAlphaComponent(color, 50));
        d.setCornerRadius(dp(c, 20));
        return d;
    }
}