package com.example.airquality;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportFragment extends Fragment {
    private LinearLayout content;
    private ReadingViewModel vm;

    private final ActivityResultLauncher<Intent> exportLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), res -> {
                if (res.getResultCode() == Activity.RESULT_OK && res.getData() != null
                        && res.getData().getData() != null) {
                    writeCsv(res.getData().getData());
                }
            });

    private String src() {
        return "live";
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle s) {
        View v = inflater.inflate(R.layout.fragment_list, container, false);
        LinearLayout list = v.findViewById(R.id.list);
        ((TextView) v.findViewById(R.id.tvHeader)).setText("Report");
        vm = new ViewModelProvider(requireActivity()).get(ReadingViewModel.class);

        content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        list.addView(content);

        Button export = new Button(requireContext());
        export.setText("Export readings as CSV");
        export.setOnClickListener(x -> {
            Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("text/csv");
            i.putExtra(Intent.EXTRA_TITLE, "air_quality_" + src() + ".csv");
            exportLauncher.launch(i);
        });
        list.addView(export);

        Button clear = new Button(requireContext());
        clear.setText("Clear stored data (" + src() + ")");
        clear.setOnClickListener(x -> new AlertDialog.Builder(requireContext())
                .setTitle("Clear stored data?")
                .setMessage("Deletes stored readings, predictions and alerts for the current mode.")
                .setPositiveButton("Delete", (d, w) -> clearData())
                .setNegativeButton("Cancel", null)
                .show());
        list.addView(clear);

        load();
        return v;
    }

    private void load() {
        final Context app = requireContext().getApplicationContext();
        final String src = src();
        final GasModel model = vm.getModel();
        final String modelErr = vm.getModelError();
        AppDatabase.IO.execute(() -> {
            AqDao dao = AppDatabase.get(app).dao();
            List<ReadingEntity> rows = dao.readingsSince(src, 0);
            List<Reading> rs = new ArrayList<>();
            for (ReadingEntity e : rows) rs.add(e.toReading());
            SimpleDateFormat f = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());

            List<String[]> sec = new ArrayList<>();
            sec.add(new String[]{"Monitored parameter",
                    "Gas / smoke index from an MQ-2 sensor on an ESP32, sent through Blynk (V0). Values are an "
                            + "approximate ppm equivalent (LPG curve) and best used as a relative indicator.\n"
                            + "Bands (app-defined): Good ≤ 50, Moderate ≤ 200, Poor ≤ 1000, Unhealthy > 1000 ppm-eq.\n"
                            + "The MQ-2 responds to several gases and smoke and cannot tell them apart."});

            String range = rows.isEmpty() ? "no readings yet"
                    : f.format(new Date(rows.get(0).ts)) + " → " + f.format(new Date(rows.get(rows.size() - 1).ts));
            sec.add(new String[]{"Stored data (mode: " + src + ")",
                    "Readings: " + rows.size() + " (" + range + ")\n"
                            + "Hourly predictions: " + dao.countPredictions(src) + "\n"
                            + "Alerts + recommendations: " + dao.countInsights(src)});

            Evaluator.Result ev = Evaluator.backtest(rs, model);
            String evText;
            if (ev.n == 0) {
                evText = "Not enough data yet. At least two consecutive hours of readings are needed.";
            } else {
                StringBuilder sb = new StringBuilder("Next-hour pairs evaluated: " + ev.n
                        + (ev.enough ? "" : "  (too few for reliable numbers; aim for 24+)") + "\n\n");
                sb.append(String.format(Locale.US, "Persistence (next = now):\n  MAE %.1f · RMSE %.1f · F1 %.2f\n",
                        ev.maePers, ev.rmsePers, ev.f1Pers));
                if (model == null) {
                    sb.append("\nModel: unavailable. ").append(modelErr);
                } else {
                    sb.append(String.format(Locale.US, "Model (%s):\n  MAE %.1f · RMSE %.1f · F1 %.2f\n",
                            model.type, ev.maeModel, ev.rmseModel, ev.f1Model));
                    if (ev.maePers > 0) {
                        double imp = 100.0 * (ev.maePers - ev.maeModel) / ev.maePers;
                        sb.append(String.format(Locale.US, "MAE vs persistence: %+.1f%% %s", imp,
                                imp > 0 ? "(model better)" : "(model not better)"));
                    }
                }
                sb.append("\n\nUnits: ppm-eq. F1 is macro-averaged over the four gas levels.");
                evText = sb.toString();
            }
            sec.add(new String[]{"Prediction accuracy (backtest on stored hourly data)", evText});

            sec.add(new String[]{"Offline training result",
                    model != null ? model.describe() : "Model unavailable. " + modelErr});
            sec.add(new String[]{"Limitations",
                    "• The model was trained on outdoor metal-oxide sensor data (UCI Air Quality) and is a baseline; "
                            + "compare it with persistence above and retrain on your own exported data.\n"
                            + "• MQ-2 readings drift with temperature, humidity and sensor age and need calibration.\n"
                            + "• Outdoor values are modelled estimates (Open-Meteo), not station measurements.\n"
                            + "• Advice is general precaution only, not a medical diagnosis."});

            View root = getView();
            if (root != null) root.post(() -> { if (isAdded()) render(sec); });
        });
    }

    private void render(List<String[]> sections) {
        content.removeAllViews();
        for (String[] s : sections) {
            TextView title = new TextView(requireContext());
            title.setText(s[0]);
            title.setTextSize(16);
            title.setTypeface(null, android.graphics.Typeface.BOLD);
            title.setPadding(0, Ui.dp(requireContext(), 16), 0, Ui.dp(requireContext(), 4));
            content.addView(title);
            TextView body = new TextView(requireContext());
            body.setText(s[1]);
            content.addView(body);
        }
    }

    private void clearData() {
        final Context app = requireContext().getApplicationContext();
        final String src = src();
        AppDatabase.IO.execute(() -> {
            AqDao dao = AppDatabase.get(app).dao();
            dao.deleteReadings(src);
            dao.deleteInsights(src);
            dao.deletePredictions(src);
            View root = getView();
            if (root != null) root.post(() -> { if (isAdded()) load(); });
        });
    }

    private void writeCsv(final Uri uri) {
        final Context app = requireContext().getApplicationContext();
        final String src = src();
        AppDatabase.IO.execute(() -> {
            String msg;
            try {
                List<ReadingEntity> rows = AppDatabase.get(app).dao().readingsSince(src, 0);
                SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                try (OutputStream os = app.getContentResolver().openOutputStream(uri);
                     Writer w = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
                    w.write("timestamp,epoch_ms,gas_ppm_eq,source\n");
                    for (ReadingEntity e : rows) {
                        w.write(f.format(new Date(e.ts)) + "," + e.ts + "," + e.gas + "," + e.source + "\n");
                    }
                }
                msg = "Exported " + rows.size() + " readings";
            } catch (Exception e) {
                msg = "Export failed: " + e.getMessage();
            }
            final String m = msg;
            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(app, m, Toast.LENGTH_LONG).show());
        });
    }
}