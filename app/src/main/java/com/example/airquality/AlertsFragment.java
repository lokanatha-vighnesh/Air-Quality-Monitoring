package com.example.airquality;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AlertsFragment extends Fragment {
    private final SimpleDateFormat fmt = new SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault());
    private String filter = "all";
    private LinearLayout items;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle s) {
        View v = inflater.inflate(R.layout.fragment_list, container, false);
        LinearLayout list = v.findViewById(R.id.list);
        ((TextView) v.findViewById(R.id.tvHeader)).setText("Alerts & Recommendations");

        ChipGroup g = new ChipGroup(requireContext());
        g.setSingleSelection(true);
        g.setSelectionRequired(true);
        addFilter(g, "All", "all", true);
        addFilter(g, "Alerts", "alert", false);
        addFilter(g, "Recommendations", "recommendation", false);
        list.addView(g);

        items = new LinearLayout(requireContext());
        items.setOrientation(LinearLayout.VERTICAL);
        list.addView(items);

        TextView note = new TextView(requireContext());
        note.setText("General precautionary advice, not a medical diagnosis. The MQ-2 is not a certified gas detector.");
        note.setTextSize(12);
        note.setPadding(0, Ui.dp(requireContext(), 12), 0, 0);
        list.addView(note);

        new ViewModelProvider(requireActivity()).get(ReadingViewModel.class)
                .getDbEvents().observe(getViewLifecycleOwner(), n -> load());
        return v;
    }

    private void addFilter(ChipGroup g, String label, String key, boolean checked) {
        Chip c = new Chip(requireContext());
        c.setText(label);
        c.setCheckable(true);
        c.setChecked(checked);
        c.setOnClickListener(x -> { filter = key; load(); });
        g.addView(c);
    }

    private void load() {
        final Context app = requireContext().getApplicationContext();
        final String src = "live";
        final String f = filter;
        final LayoutInflater inf = getLayoutInflater();
        AppDatabase.IO.execute(() -> {
            List<InsightEntity> all = AppDatabase.get(app).dao().insights(src, 200);
            final List<InsightEntity> shown = new ArrayList<>();
            for (InsightEntity e : all) if (f.equals("all") || f.equals(e.kind)) shown.add(e);
            View root = getView();
            if (root != null) root.post(() -> { if (isAdded()) render(shown, inf); });
        });
    }

    private void render(List<InsightEntity> shown, LayoutInflater inf) {
        items.removeAllViews();
        if (shown.isEmpty()) {
            TextView t = new TextView(requireContext());
            t.setText("Nothing recorded yet.");
            t.setPadding(0, Ui.dp(requireContext(), 12), 0, 0);
            items.addView(t);
            return;
        }
        for (InsightEntity e : shown) {
            View c = inf.inflate(R.layout.item_card, items, false);
            Level lv = Level.values()[Math.max(0, Math.min(e.level, Level.values().length - 1))];
            ((TextView) c.findViewById(R.id.tvTitle)).setText(e.title);
            ((TextView) c.findViewById(R.id.tvBody)).setText(e.message);
            ((TextView) c.findViewById(R.id.tvMeta)).setText(
                    ("alert".equals(e.kind) ? "Alert · " : "Recommendation · ") + fmt.format(new Date(e.ts)));
            TextView chip = c.findViewById(R.id.tvChip);
            chip.setVisibility(View.VISIBLE);
            chip.setText(lv.label);
            chip.setBackground(Ui.pill(requireContext(), lv.color));
            items.addView(c);
        }
    }
}