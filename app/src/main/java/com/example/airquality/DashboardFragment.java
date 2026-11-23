package com.example.airquality;

import android.content.Context;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Home screen: indoor gauge, outdoor pollutants, trend, forecast, advisory. Real data only. */
public class DashboardFragment extends Fragment {
    private static final int GREY = 0xFF9CA3AF, GREEN = 0xFF16A34A, MUTED = 0xFF6B7280, DARK = 0xFF111827;
    private static final long MIN = 60_000L;

    private final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private final SimpleDateFormat axisFmt = new SimpleDateFormat("h:mm a", Locale.getDefault());

    private GaugeView gauge;
    private TextView tvGasValue, tvGasLevel, tvStatus, tvHeroSub, tvUpdated;
    private TextView tvOutSummary, tvCompare, tvOutdoorStatus;
    private LinearLayout rowPoll1, rowPoll2;
    private TextView tvR1, tvR6, tvR24, tvTrendEmpty;
    private TrendChartView trendChart;
    private TextView tvFcValue, tvFcLevel, tvFcNote, tvFcModel, tvAdvisory;
    private MaterialCardView cardAdvisory;
    private ImageView imgShield;

    private List<Reading> hist;
    private boolean connected;
    private Forecaster.Forecast fc;
    private OutdoorData outdoor;
    private String sourceLabel = "", outStatus = "";
    private int rangeMin = 60;
    private long lastTrendLoad;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle s) {
        View v = inflater.inflate(R.layout.fragment_dashboard, container, false);

        TextView tvTitle = v.findViewById(R.id.tvTitle);
        SpannableString title = new SpannableString("AirSense");
        title.setSpan(new ForegroundColorSpan(DARK), 0, 3, 0);
        title.setSpan(new ForegroundColorSpan(GREEN), 3, 8, 0);
        tvTitle.setText(title);

        String place = AppPrefs.getPlace(requireContext());
        ((TextView) v.findViewById(R.id.tvPlace)).setText(place.isEmpty() ? "Set location" : place);
        v.findViewById(R.id.layoutPlace).setOnClickListener(x ->
                ((MainActivity) requireActivity()).selectTab(R.id.nav_map));
        v.findViewById(R.id.btnSettings).setOnClickListener(x ->
                ((MainActivity) requireActivity()).openSettings());

        gauge = v.findViewById(R.id.gauge);
        tvGasValue = v.findViewById(R.id.tvGasValue);
        tvGasLevel = v.findViewById(R.id.tvGasLevel);
        tvStatus = v.findViewById(R.id.tvStatus);
        tvHeroSub = v.findViewById(R.id.tvHeroSub);
        tvUpdated = v.findViewById(R.id.tvUpdated);
        tvOutSummary = v.findViewById(R.id.tvOutSummary);
        tvCompare = v.findViewById(R.id.tvCompare);
        tvOutdoorStatus = v.findViewById(R.id.tvOutdoorStatus);
        rowPoll1 = v.findViewById(R.id.rowPoll1);
        rowPoll2 = v.findViewById(R.id.rowPoll2);
        tvR1 = v.findViewById(R.id.tvR1);
        tvR6 = v.findViewById(R.id.tvR6);
        tvR24 = v.findViewById(R.id.tvR24);
        tvTrendEmpty = v.findViewById(R.id.tvTrendEmpty);
        trendChart = v.findViewById(R.id.trendChart);
        tvFcValue = v.findViewById(R.id.tvFcValue);
        tvFcLevel = v.findViewById(R.id.tvFcLevel);
        tvFcNote = v.findViewById(R.id.tvFcNote);
        tvFcModel = v.findViewById(R.id.tvFcModel);
        cardAdvisory = v.findViewById(R.id.cardAdvisory);
        imgShield = v.findViewById(R.id.imgShield);
        tvAdvisory = v.findViewById(R.id.tvAdvisory);

        cardAdvisory.setOnClickListener(x -> ((MainActivity) requireActivity()).selectTab(R.id.nav_alerts));
        tvR1.setOnClickListener(x -> setRange(60));
        tvR6.setOnClickListener(x -> setRange(360));
        tvR24.setOnClickListener(x -> setRange(1440));
        setRange(rangeMin);

        ViewModelProvider p = new ViewModelProvider(requireActivity());
        ReadingViewModel vm = p.get(ReadingViewModel.class);
        OutdoorViewModel ovm = p.get(OutdoorViewModel.class);
        vm.getHistory().observe(getViewLifecycleOwner(), h -> {
            hist = h;
            renderAll();
            if (System.currentTimeMillis() - lastTrendLoad > 15_000) loadTrend();
        });
        vm.getConnected().observe(getViewLifecycleOwner(), c -> { connected = Boolean.TRUE.equals(c); renderAll(); });
        vm.getStatus().observe(getViewLifecycleOwner(), st -> { sourceLabel = st; renderAll(); });
        vm.getForecast().observe(getViewLifecycleOwner(), f -> { fc = f; renderAll(); });
        ovm.getData().observe(getViewLifecycleOwner(), d -> { outdoor = d; renderAll(); });
        ovm.getStatus().observe(getViewLifecycleOwner(), st -> { outStatus = st; renderAll(); });
        return v;
    }

    // ---------------------------------------------------------------- render
    private void renderAll() {
        if (!isAdded() || getView() == null) return;
        Reading now = (connected && hist != null && !hist.isEmpty()) ? hist.get(hist.size() - 1) : null;
        Forecaster.Forecast usable = (fc != null && fc.error == null) ? fc : null;
        renderHero(now);
        renderOutdoor(now);
        renderForecast(now);
        renderAdvisory(now, usable);
    }

    private void renderHero(Reading now) {
        if (now == null) {
            gauge.set(0f, GREY);
            tvGasValue.setText("—");
            tvGasValue.setTextColor(GREY);
            tvGasLevel.setVisibility(View.GONE);
            tvStatus.setText("Not connected");
            tvStatus.setTextColor(GREY);
            tvHeroSub.setText(sourceLabel);
            tvUpdated.setText("");
            return;
        }
        Level lv = now.overall();
        gauge.set(Param.gaugeFraction(now.gas), lv.color);
        tvGasValue.setText(Param.GAS.format(now.gas));
        tvGasValue.setTextColor(DARK);
        tvGasLevel.setVisibility(View.VISIBLE);
        tvGasLevel.setText(lv.label);
        tvGasLevel.setTextColor(lv.color);
        tvGasLevel.setBackground(Ui.pill(requireContext(), lv.color));
        tvStatus.setText(lv == Level.GOOD ? "Good!" : lv.label);
        tvStatus.setTextColor(lv.color);
        tvHeroSub.setText(heroSub(lv));
        tvUpdated.setText("Updated " + timeFmt.format(new Date(now.timestamp)));
    }

    private String heroSub(Level lv) {
        switch (lv) {
            case GOOD: return "Enjoy fresh indoor air.";
            case MODERATE: return "Slightly raised. Consider ventilating.";
            case POOR: return "Elevated. Ventilate and check for sources.";
            default: return "Very high! Ventilate; leave if you smell gas.";
        }
    }

    private void renderOutdoor(Reading now) {
        tvOutdoorStatus.setText(outStatus);
        Level ol = Insights.outdoorLevel(outdoor);
        if (outdoor == null) {
            tvOutSummary.setText("No outdoor data yet. Pick your location on the Map tab.");
        } else {
            tvOutSummary.setText("Outdoor AQI " + (outdoor.usAqi >= 0 ? String.valueOf(outdoor.usAqi) : "—")
                    + (ol != null ? " · " + ol.label : ""));
        }
        if (now != null && ol != null) {
            tvCompare.setVisibility(View.VISIBLE);
            tvCompare.setText("Indoor: " + now.overall().label + "   ·   Outdoor: " + ol.label);
        } else {
            tvCompare.setVisibility(View.GONE);
        }
        rowPoll1.removeAllViews();
        rowPoll2.removeAllViews();
        addTile(rowPoll1, "PM2.5", outdoor == null ? Float.NaN : outdoor.pm25, 0);
        addTile(rowPoll1, "PM10", outdoor == null ? Float.NaN : outdoor.pm10, 0);
        addTile(rowPoll1, "NO₂", outdoor == null ? Float.NaN : outdoor.no2, 0);
        addTile(rowPoll2, "SO₂", outdoor == null ? Float.NaN : outdoor.so2, 0);
        addTile(rowPoll2, "CO (mg/m³)", outdoor == null ? Float.NaN : outdoor.co / 1000f, 1);
        addTile(rowPoll2, "O₃", outdoor == null ? Float.NaN : outdoor.o3, 0);
    }

    private void addTile(LinearLayout row, String label, float value, int decimals) {
        View t = getLayoutInflater().inflate(R.layout.tile_pollutant, row, false);
        ((TextView) t.findViewById(R.id.tvTileLabel)).setText(label);
        ((TextView) t.findViewById(R.id.tvTileValue)).setText(Float.isNaN(value) ? "—"
                : String.format(Locale.US, "%." + decimals + "f", value));
        row.addView(t);
    }

    private void renderForecast(Reading now) {
        tvFcLevel.setVisibility(View.GONE);
        tvFcValue.setTextColor(GREY);
        tvFcModel.setText("");
        if (now == null) {
            tvFcValue.setText("—");
            tvFcNote.setText("Forecast needs a live sensor connection.");
            return;
        }
        if (fc == null) {
            tvFcValue.setText("—");
            tvFcNote.setText("Calculating forecast…");
            return;
        }
        if (fc.error != null) {
            tvFcValue.setText("—");
            tvFcNote.setText(fc.error);
            return;
        }
        tvFcValue.setText(Param.GAS.format(fc.gas) + " ppm-eq");
        tvFcValue.setTextColor(fc.level.color);
        tvFcLevel.setVisibility(View.VISIBLE);
        tvFcLevel.setText(fc.level.label);
        tvFcLevel.setTextColor(fc.level.color);
        tvFcLevel.setBackground(Ui.pill(requireContext(), fc.level.color));

        String trend = "► Steady";
        if (fc.gas > now.gas * 1.2f + 2f) trend = "▲ Rising";
        else if (fc.gas < now.gas / 1.2f - 2f) trend = "▼ Falling";
        String note = trend + " compared with now";
        if (fc.lowConfidence) {
            note += "\nLow confidence: still collecting hourly history (" + fc.found + "/5 hours).";
        }
        tvFcNote.setText(note);
        tvFcModel.setText(fc.modelInfo);
    }

    private void renderAdvisory(Reading now, Forecaster.Forecast usable) {
        if (now == null) {
            cardAdvisory.setCardBackgroundColor(ColorUtils.setAlphaComponent(GREY, 30));
            imgShield.setColorFilter(GREY);
            tvAdvisory.setText("Connect your ESP32 (via Blynk) to get advice.");
            return;
        }
        Level lv = now.overall();
        cardAdvisory.setCardBackgroundColor(ColorUtils.setAlphaComponent(lv.color, 35));
        imgShield.setColorFilter(lv.color);
        tvAdvisory.setText(Insights.build(now, outdoor, usable).get(0));
    }

    // ---------------------------------------------------------------- trend (stored readings)
    private void setRange(int minutes) {
        rangeMin = minutes;
        styleRange(tvR1, minutes == 60);
        styleRange(tvR6, minutes == 360);
        styleRange(tvR24, minutes == 1440);
        loadTrend();
    }

    private void styleRange(TextView t, boolean selected) {
        t.setBackgroundResource(selected ? R.drawable.bg_range_selected : R.drawable.bg_range_unselected);
        t.setTextColor(selected ? 0xFFFFFFFF : MUTED);
    }

    private void loadTrend() {
        lastTrendLoad = System.currentTimeMillis();
        final Context app = requireContext().getApplicationContext();
        final int minutes = rangeMin;
        AppDatabase.IO.execute(() -> {
            long nowMs = System.currentTimeMillis();
            List<ReadingEntity> rows = AppDatabase.get(app).dao().readingsSince("live", nowMs - minutes * MIN);
            final int n = rows.size();
            if (n == 0) {
                trendChart.post(() -> showTrend(null, 0, null));
                return;
            }
            int step = Math.max(1, (int) Math.ceil(n / 200.0));
            final float[] pts = new float[(n + step - 1) / step];
            for (int i = 0; i < pts.length; i++) {
                float sum = 0;
                int c = 0;
                for (int j = i * step; j < Math.min(n, (i + 1) * step); j++) { sum += rows.get(j).gas; c++; }
                pts[i] = sum / c;
            }
            long t0 = rows.get(0).ts, t1 = rows.get(n - 1).ts;
            final String[] labels = new String[4];
            for (int i = 0; i < 4; i++) labels[i] = axisFmt.format(new Date(t0 + (t1 - t0) * i / 3));
            final int color = Param.GAS.classify(rows.get(n - 1).gas).color;
            trendChart.post(() -> showTrend(pts, color, labels));
        });
    }

    private void showTrend(float[] pts, int color, String[] labels) {
        if (!isAdded()) return;
        if (pts == null) {
            tvTrendEmpty.setVisibility(View.VISIBLE);
            tvTrendEmpty.setText("No stored readings for this range yet.");
            trendChart.setData(new float[0], GREEN, new String[0]);
        } else {
            tvTrendEmpty.setVisibility(View.GONE);
            trendChart.setData(pts, color, labels);
        }
    }
}