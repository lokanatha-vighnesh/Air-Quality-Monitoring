package com.example.airquality;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/** 240-degree arc gauge, open at the bottom. */
public class GaugeView extends View {
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float stroke;
    private float fraction = 0f;

    public GaugeView(Context c, AttributeSet a) {
        super(c, a);
        stroke = Ui.dp(c, 14);
        for (Paint p : new Paint[]{track, arc}) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(stroke);
            p.setStrokeCap(Paint.Cap.ROUND);
        }
        track.setColor(0xFFE5E7EB);
        arc.setColor(0xFF9CA3AF);
    }

    public void set(float fraction, int color) {
        this.fraction = Math.max(0f, Math.min(1f, fraction));
        arc.setColor(color);
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float size = Math.min(getWidth(), getHeight());
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float r = size / 2f - stroke;
        rect.set(cx - r, cy - r, cx + r, cy + r);
        canvas.drawArc(rect, 150f, 240f, false, track);
        if (fraction > 0.001f) canvas.drawArc(rect, 150f, 240f * fraction, false, arc);
    }
}
