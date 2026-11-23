package com.example.airquality;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Live readings from Blynk only. No data is ever simulated: when Blynk cannot deliver a reading,
 * the app is simply "not connected". Also stores readings in Room, forecasts and raises alerts.
 */
public class ReadingViewModel extends AndroidViewModel {
    private static final String SRC = "live";
    private static final int MAX_POINTS = 120;
    private static final long H = 3600000L;
    private static final long INTERVAL_MS = 5000, STORE_EVERY_MS = 60_000L,
            FORECAST_EVERY_MS = 30_000L, ALERT_COOLDOWN_MS = 15 * 60_000L;

    private final MutableLiveData<List<Reading>> history = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> status = new MutableLiveData<>("Not connected");
    private final MutableLiveData<Boolean> connected = new MutableLiveData<>(false);
    private final MutableLiveData<Forecaster.Forecast> forecast = new MutableLiveData<>();
    private final MutableLiveData<Integer> dbEvents = new MutableLiveData<>(0);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService net = Executors.newSingleThreadExecutor();
    private final AtomicBoolean busy = new AtomicBoolean(false);
    private final AqDao dao;

    private GasModel model;
    private String modelError;
    private long lastStored, lastForecast;

    // touched only on AppDatabase.IO
    private final Map<String, Long> lastAlert = new HashMap<>();
    private long lastPredHour = -1;
    private int eventCount = 0;
    private volatile Forecaster.Forecast lastFc;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            fetchLive();
            handler.postDelayed(this, INTERVAL_MS);
        }
    };

    public ReadingViewModel(@NonNull Application app) {
        super(app);
        dao = AppDatabase.get(app).dao();
        try {
            model = GasModel.load(app);
        } catch (Exception e) {
            modelError = "Forecast model missing or invalid (" + e.getMessage()
                    + "). Run train_model.py and copy aq_model.json to app/src/main/assets/.";
        }
        AppDatabase.IO.execute(() -> {
            dao.pruneReadings(System.currentTimeMillis() - 30L * 24 * H);
            // remove anything left over from the old simulated mode
            dao.deleteReadings("sim");
            dao.deleteInsights("sim");
            dao.deletePredictions("sim");
        });
        handler.post(tick);
    }

    public LiveData<List<Reading>> getHistory() { return history; }
    public LiveData<String> getStatus() { return status; }
    public LiveData<Boolean> getConnected() { return connected; }
    public LiveData<Forecaster.Forecast> getForecast() { return forecast; }
    public LiveData<Integer> getDbEvents() { return dbEvents; }
    public GasModel getModel() { return model; }
    public String getModelError() { return modelError; }

    /** Called after Settings are saved. */
    public void refreshNow() {
        handler.removeCallbacks(tick);
        handler.post(tick);
    }

    private void setDisconnected(String msg) {
        connected.setValue(false);
        status.setValue(msg);
        forecast.setValue(null);
        lastFc = null;
    }

    private void fetchLive() {
        final String token = AppPrefs.getToken(getApplication()).trim();
        final String server = AppPrefs.getServer(getApplication());
        if (token.isEmpty()) {
            setDisconnected("Not connected: add your Blynk device token in Settings");
            return;
        }
        if (!busy.compareAndSet(false, true)) return;
        if (!Boolean.TRUE.equals(connected.getValue())) status.setValue("Connecting to Blynk…");
        net.execute(() -> {
            BlynkClient.Result r = BlynkClient.fetch(server, token);
            handler.post(() -> { applyLive(r); busy.set(false); });
        });
    }

    private void applyLive(BlynkClient.Result r) {
        if (r.error != null) {
            setDisconnected("Not connected: " + r.error);
            return;
        }
        if (!r.deviceOnline) {
            setDisconnected("Not connected: the ESP32 is offline (not reaching Blynk)");
            return;
        }
        List<Reading> cur = new ArrayList<>(history.getValue());
        cur.add(r.reading);
        while (cur.size() > MAX_POINTS) cur.remove(0);
        history.setValue(cur);
        connected.setValue(true);
        status.setValue("Live · Blynk");
        onReading(r.reading);
    }

    // ---------------------------------------------------------------- storage, forecast, alerts
    private void onReading(final Reading r) {
        final boolean store = r.timestamp - lastStored >= STORE_EVERY_MS;
        if (store) lastStored = r.timestamp;
        final boolean doFc = r.timestamp - lastForecast >= FORECAST_EVERY_MS;
        if (doFc) lastForecast = r.timestamp;

        AppDatabase.IO.execute(() -> {
            if (store) dao.insertReading(new ReadingEntity(r.timestamp, r.gas, SRC));
            if (doFc) {
                Forecaster.Forecast f = computeForecast(r);
                lastFc = f;
                forecast.postValue(f);
                logPrediction(r, f);
            }
            checkAlerts(r, lastFc);
        });
    }

    private Forecaster.Forecast computeForecast(Reading r) {
        Forecaster.Forecast f;
        if (model == null) {
            f = new Forecaster.Forecast();
            f.error = modelError;
            f.level = Level.GOOD;
            return f;
        }
        List<ReadingEntity> rows = dao.readingsSince(SRC, r.timestamp - 8 * H);
        List<Reading> rs = new ArrayList<>();
        for (ReadingEntity e : rows) rs.add(e.toReading());
        f = Forecaster.predict(model, rs, r);
        if (f.found < 2) {                       // not enough real history yet
            Forecaster.Forecast w = new Forecaster.Forecast();
            w.error = "Collecting hourly history from the sensor (" + f.found + "/5 hours)…";
            w.level = Level.GOOD;
            return w;
        }
        f.modelInfo = model.describe();
        return f;
    }

    /** One stored prediction + recommendation per clock hour. */
    private void logPrediction(Reading r, Forecaster.Forecast f) {
        if (f == null || f.error != null) return;
        long hour = r.timestamp / H * H;
        if (hour == lastPredHour) return;
        List<PredictionEntity> last = dao.predictions(SRC, 1);
        lastPredHour = hour;
        if (!last.isEmpty() && last.get(0).ts / H * H == hour) return;

        dao.insertPrediction(new PredictionEntity(r.timestamp, f.gas, r.gas, SRC));
        OutdoorData out = OutdoorData.fromJson(AppPrefs.getOutdoorCache(getApplication()));
        StringBuilder sb = new StringBuilder();
        for (String t : Insights.build(r, out, f)) sb.append(t).append(' ');
        dao.insertInsight(new InsightEntity(r.timestamp, "recommendation", "Hourly recommendation",
                sb.toString().trim(), r.overall().ordinal(), SRC));
        dbEvents.postValue(++eventCount);
    }

    private void checkAlerts(Reading r, Forecaster.Forecast f) {
        for (AlertRules.Alert a : AlertRules.evaluate(r, f)) {
            Long last = lastAlert.get(a.key);
            if (last != null && r.timestamp - last < ALERT_COOLDOWN_MS) continue;
            lastAlert.put(a.key, r.timestamp);
            dao.insertInsight(new InsightEntity(r.timestamp, "alert", a.title, a.message, a.level.ordinal(), SRC));
            Notifier.notify(getApplication(), a.title, a.message, a.key.hashCode());
            dbEvents.postValue(++eventCount);
        }
    }

    @Override protected void onCleared() {
        handler.removeCallbacks(tick);
        net.shutdownNow();
    }
}