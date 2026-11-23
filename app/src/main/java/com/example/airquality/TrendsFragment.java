package com.example.airquality;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TrendsFragment extends Fragment {
    private static final long H = 3600000L;
    private int rangeHours = 24;
    private long lastLoad;
    private LineChartView chart;
    private TextView tvTitle, tvMin, tvAvg, tvMax, tvStart, tvEnd, tvEmpty, tvShares;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle s) {
        View v = inflater.inflate(R.layout.fragment_trends, container, false);
        chart = v.findViewById(R.id.chart);
        tvTitle = v.findViewById(R.id.tvChartTitle);
        tvMin = v.findViewById(R.id.tvMin);
        tvAvg = v.findViewById(R.id.tvAvg);
        tvMax = v.findViewById(R.id.tvMax);
        tvStart = v.findViewById(R.id.tvStart);
        tvEnd = v.findViewById(R.id.tvEnd);
        tvEmpty = v.findViewById(R.id.tvEmpty);
        tvShares = v.findViewById(R.id.tvShares);

        ChipGroup group = v.findViewById(R.id.chipGroup);
        group.setSingleSelection(true);
        group.setSelectionRequired(true);
        String[] names = {"1 h", "24 h", "7 d"};
        int[] hours = {1, 24, 168};
        for (int i = 0; i < names.length; i++) {
            final int hrs = hours[i];
            Chip chip = new Chip(requireContext());
            chip.setText(names[i]);
            chip.setCheckable(true);
            chip.setChecked(hrs == rangeHours);
            chip.setOnClickListener(x -> { rangeHours = hrs; load(); });
            group.addView(chip);
        }

        ReadingViewModel vm = new ViewModelProvider(requireActivity()).get(ReadingViewModel.class);
        vm.getHistory().observe(getViewLifecycleOwner(), h -> {
            if (System.currentTimeMillis() - lastLoad > 10_000) load();
        });
        load();
        return v;
    }

    private void load() {
        lastLoad = System.currentTimeMillis();
        final Context app = requireContext().getApplicationContext();
        final String src = "live";
        final int hrs = rangeHours;
        AppDatabase.IO.execute(() -> {
            List<ReadingEntity> rows = AppDatabase.get(app).dao()
                    .readingsSince(src, System.currentTimeMillis() - hrs * H);
            final int n = rows.size();
            if (n == 0) { post(() -> showEmpty()); return; }

            int step = Math.max(1, (int) Math.ceil(n / 300.0));          // downsample for drawing
            final float[] pts = new float[(n + step - 1) / step];
            for (int i = 0; i < pts.length; i++) {
                float sum = 0; int c = 0;
                for (int j = i * step; j < Math.min(n, (i + 1) * step); j++) { sum += rows.get(j).gas; c++; }
                pts[i] = sum / c;
            }
            float min = Float.MAX_VALUE, max = -Float.MAX_VALUE, total = 0;
            int[] share = new int[Level.values().length];
            for (ReadingEntity e : rows) {
                min = Math.min(min, e.gas);
                max = Math.max(max, e.gas);
                total += e.gas;
                share[Param.GAS.classify(e.gas).ordinal()]++;
            }
            final float fMin = min, fMax = max, fAvg = total / n;
            StringBuilder sb = new StringBuilder("Time in each level: ");
            for (Level l : Level.values()) {
                sb.append(l.label).append(' ').append(Math.round(100f * share[l.ordinal()] / n)).append("%  ");
            }
            final String shares = sb.toString().trim();
            final long t0 = rows.get(0).ts, t1 = rows.get(n - 1).ts;
            final int color = Param.GAS.classify(rows.get(n - 1).gas).color;
            post(() -> show(pts, color, fMin, fAvg, fMax, shares, t0, t1, hrs));
        });
    }

    private void post(Runnable r) {
        View v = getView();
        if (v != null) v.post(() -> { if (isAdded()) r.run(); });
    }

    private void showEmpty() {
        tvEmpty.setVisibility(View.VISIBLE);
        tvEmpty.setText("No stored readings for this range yet.");
        chart.setData(new float[0], 0);
        tvMin.setText(""); tvAvg.setText(""); tvMax.setText("");
        tvStart.setText(""); tvEnd.setText(""); tvShares.setText("");
    }

    private void show(float[] pts, int color, float min, float avg, float max, String shares,
                      long t0, long t1, int hrs) {
        SimpleDateFormat f = new SimpleDateFormat(hrs > 24 ? "dd MMM HH:mm" : "HH:mm", Locale.getDefault());
        tvEmpty.setVisibility(View.GONE);
        chart.setData(pts, color);
        tvTitle.setText("Gas / smoke index (" + Param.GAS.unit + ")");
        tvMin.setText("Min\n" + Param.GAS.format(min));
        tvAvg.setText("Avg\n" + Param.GAS.format(avg));
        tvMax.setText("Max\n" + Param.GAS.format(max));
        tvStart.setText(f.format(new Date(t0)));
        tvEnd.setText(f.format(new Date(t1)));
        tvShares.setText(shares);
    }
}