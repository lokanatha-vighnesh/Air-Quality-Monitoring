package com.example.airquality;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Fetches outdoor weather/air quality every 10 minutes; falls back to cached data on failure. */
public class OutdoorViewModel extends AndroidViewModel {
    private static final long INTERVAL_MS = 10 * 60 * 1000L;

    private final MutableLiveData<OutdoorData> data = new MutableLiveData<>();
    private final MutableLiveData<String> status = new MutableLiveData<>("");
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean busy = new AtomicBoolean(false);
    private final SimpleDateFormat fmt = new SimpleDateFormat("HH:mm", Locale.getDefault());

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            refresh();
            handler.postDelayed(this, INTERVAL_MS);
        }
    };

    public OutdoorViewModel(@NonNull Application app) {
        super(app);
        OutdoorData c = OutdoorData.fromJson(AppPrefs.getOutdoorCache(app));
        if (c != null) {
            c.cached = true;
            data.setValue(c);
        }
        handler.post(tick);
    }

    public LiveData<OutdoorData> getData() { return data; }
    public LiveData<String> getStatus() { return status; }

    /** Called after Settings change. */
    public void refreshNow() {
        handler.removeCallbacks(tick);
        handler.post(tick);
    }

    private double[] parseLocation() {
        try {
            double la = Double.parseDouble(AppPrefs.getLat(getApplication()).trim());
            double lo = Double.parseDouble(AppPrefs.getLon(getApplication()).trim());
            if (la < -90 || la > 90 || lo < -180 || lo > 180) return null;
            return new double[]{la, lo};
        } catch (Exception e) {
            return null;
        }
    }

    private void refresh() {
        final double[] loc = parseLocation();
        if (loc == null) {
            status.setValue("Set your latitude/longitude in Settings to load outdoor data");
            return;
        }
        if (!busy.compareAndSet(false, true)) return;
        status.setValue("Fetching outdoor data…");
        executor.execute(() -> {
            OpenMeteoClient.Result r = OpenMeteoClient.fetch(loc[0], loc[1]);
            handler.post(() -> { apply(r); busy.set(false); });
        });
    }

    private void apply(OpenMeteoClient.Result r) {
        if (r.data != null) {
            r.data.cached = false;
            AppPrefs.setOutdoorCache(getApplication(), r.data.toJson());
            data.setValue(r.data);
            status.setValue("Outdoor data updated " + fmt.format(new Date(r.data.fetchedAt)));
            return;
        }
        OutdoorData cur = data.getValue();
        if (cur != null) {
            cur.cached = true;
            data.setValue(cur);
            status.setValue("Offline · showing cached outdoor data from "
                    + fmt.format(new Date(cur.fetchedAt)) + " (" + r.error + ")");
        } else {
            status.setValue("Outdoor data unavailable: " + r.error);
        }
    }

    @Override protected void onCleared() {
        handler.removeCallbacks(tick);
        executor.shutdownNow();
    }
}