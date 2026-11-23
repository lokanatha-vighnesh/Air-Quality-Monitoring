package com.example.airquality;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import java.util.Locale;

public class TrendChartView extends View {
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint grid = new Paint();
    private final Paint txt = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path(), area = new Path();
    private float[] v = new float[0];
    private String[] xl = new String[0];
    private int color = 0xFF16A34A;

    public TrendChartView(Context c, AttributeSet a) {
        super(c, a);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(Ui.dp(c, 2));
        line.setStrokeCap(Paint.Cap.ROUND);
        line.setStrokeJoin(Paint.Join.ROUND);
        fill.setStyle(Paint.Style.FILL);
        grid.setColor(0xFFE5E7EB);
        grid.setStrokeWidth(Ui.dp(c, 1));
        txt.setColor(0xFF4B5563);
        txt.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 11, c.getResources().getDisplayMetrics()));
        dot.setStyle(Paint.Style.FILL);
    }

    public void setData(float[] values, int color, String[] xLabels) {
        this.v = values == null ? new float[0] : values;
        this.xl = xLabels == null ? new String[0] : xLabels;
        this.color = color;
        invalidate();
    }

    private static float niceMax(float max) {
        if (max <= 0f) return 10f;
        double mag = Math.pow(10, Math.floor(Math.log10(max)));
        double n = max / mag;
        double nice = n <= 1 ? 1 : n <= 2 ? 2 : n <= 5 ? 5 : 10;
        return (float) (nice * mag);
    }

    private static String fmt(float val, float ymax) {
        return ymax < 10f ? String.format(Locale.US, "%.1f", val) : String.format(Locale.US, "%.0f", val);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Context c = getContext();
        float L = Ui.dp(c, 36), R = Ui.dp(c, 12), T = Ui.dp(c, 8), B = Ui.dp(c, 28);
        float w = getWidth() - L - R, h = getHeight() - T - B;
        float max = 0f;
        for (float x : v) max = Math.max(max, x);
        float ymax = niceMax(max);

        txt.setTextAlign(Paint.Align.RIGHT);
        for (int i = 0; i < 3; i++) {
            float y = T + h * i / 2f;
            canvas.drawLine(L, y, getWidth() - R, y, grid);
            canvas.drawText(fmt(ymax * (2 - i) / 2f, ymax), L - Ui.dp(c, 6), y + Ui.dp(c, 4), txt);
        }
        txt.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < xl.length; i++) {
            float x = L + w * i / Math.max(1, xl.length - 1);
            float half = txt.measureText(xl[i]) / 2f;
            x = Math.max(half, Math.min(getWidth() - half, x));
            canvas.drawText(xl[i], x, getHeight() - Ui.dp(c, 6), txt);
        }

        int n = v.length;
        if (n == 0) return;
        float[] px = new float[n], py = new float[n];
        for (int i = 0; i < n; i++) {
            px[i] = n == 1 ? L + w : L + w * i / (n - 1);
            py[i] = T + h * (1f - Math.min(v[i], ymax) / ymax);
        }
        path.reset();
        path.moveTo(px[0], py[0]);
        for (int i = 0; i < n - 1; i++) {
            path.quadTo(px[i], py[i], (px[i] + px[i + 1]) / 2f, (py[i] + py[i + 1]) / 2f);
        }
        path.lineTo(px[n - 1], py[n - 1]);

        area.set(path);
        area.lineTo(px[n - 1], T + h);
        area.lineTo(px[0], T + h);
        area.close();
        fill.setShader(new LinearGradient(0, T, 0, T + h,
                (color & 0x00FFFFFF) | 0x55000000, color & 0x00FFFFFF, Shader.TileMode.CLAMP));
        canvas.drawPath(area, fill);

        line.setColor(color);
        canvas.drawPath(path, line);
        dot.setColor(color);
        canvas.drawCircle(px[n - 1], py[n - 1], Ui.dp(c, 5), dot);
    }
}
