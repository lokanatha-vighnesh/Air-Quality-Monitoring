package com.example.airquality;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

public class LineChartView extends View {
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint();
    private final Path path = new Path();
    private float[] values = new float[0];

    public LineChartView(Context c, AttributeSet a) {
        super(c, a);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(Ui.dp(c, 2));
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        gridPaint.setColor(0x33888888);
        gridPaint.setStrokeWidth(1f);
    }

    public void setData(float[] values, int color) {
        this.values = values;
        linePaint.setColor(color);
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (values.length < 2) return;
        float min = values[0], max = values[0];
        for (float v : values) { min = Math.min(min, v); max = Math.max(max, v); }
        float range = Math.max(max - min, 0.001f);
        float pad = 8f;
        float w = getWidth() - pad * 2, h = getHeight() - pad * 2;

        for (int i = 0; i < 4; i++) {
            float y = pad + h * i / 3f;
            canvas.drawLine(pad, y, getWidth() - pad, y, gridPaint);
        }
        path.reset();
        for (int i = 0; i < values.length; i++) {
            float x = pad + w * i / (values.length - 1);
            float y = pad + h * (1 - (values[i] - min) / range);
            if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
        }
        canvas.drawPath(path, linePaint);
    }
}