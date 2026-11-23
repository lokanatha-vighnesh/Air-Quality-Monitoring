package com.example.airquality;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.util.MapTileIndex;
import org.osmdroid.views.overlay.CopyrightOverlay;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Pick the outdoor location by tapping the map (OpenStreetMap) or searching for a place. */
public class MapFragment extends Fragment {
    private static final String MAPTILER_KEY = BuildConfig.MAPTILER_KEY;
    private MapView map;
    private Marker marker;
    private double selLat = Double.NaN, selLon = Double.NaN;
    private String selName = "";
    private TextView tvSelected;
    private EditText etSearch;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle s) {
        Context app = requireContext().getApplicationContext();

// Load osmdroid configuration
        Configuration.getInstance().load(
                app,
                app.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        );

// Set a unique application User-Agent
        Configuration.getInstance().setUserAgentValue(
                "AirQualityApp/1.0 (Android; contact: lokanathavighnesh@gmail.com)"
        );

// Limit simultaneous tile downloads
        Configuration.getInstance().setTileDownloadThreads((short) 2);

        View v = inflater.inflate(R.layout.fragment_map, container, false);
        map = v.findViewById(R.id.map);
        tvSelected = v.findViewById(R.id.tvSelected);
        etSearch = v.findViewById(R.id.etSearch);
        map.setTileSource(maptilerSource());
        map.getOverlays().add(new CopyrightOverlay(requireContext()));
        map.setTilesScaledToDpi(true);   // scales tiles with screen density, so street names get bigger
        map.setMinZoomLevel(3.0);        // stops the map zooming out so far that labels become unreadable
        map.setMaxZoomLevel(30.0);// required attribution
        map.setTilesScaleFactor(1.5f);
        if (MAPTILER_KEY.isEmpty()) {
            Toast.makeText(requireContext(), "MapTiler key missing: add MAPTILER_KEY to local.properties", Toast.LENGTH_LONG).show();
        }
        map.setMultiTouchControls(true);

        map.getOverlays().add(new MapEventsOverlay(new MapEventsReceiver() {
            @Override public boolean singleTapConfirmedHelper(GeoPoint p) {
                select(p.getLatitude(), p.getLongitude());
                return true;
            }
            @Override public boolean longPressHelper(GeoPoint p) {
                select(p.getLatitude(), p.getLongitude());
                return true;
            }
        }));

        double la = parse(AppPrefs.getLat(app)), lo = parse(AppPrefs.getLon(app));
        if (!Double.isNaN(la) && !Double.isNaN(lo)) {
            map.getController().setZoom(15.0);
            map.getController().setCenter(new GeoPoint(la, lo));
            showSelection(la, lo, AppPrefs.getPlace(app));
        } else {
            map.getController().setZoom(2.5);
            map.getController().setCenter(new GeoPoint(20.0, 0.0));
            tvSelected.setText("Tap the map to choose your location, or search for a place.");
        }
        v.findViewById(R.id.btnSearch).setOnClickListener(x -> search());
        v.findViewById(R.id.btnUse).setOnClickListener(x -> useLocation());
        return v;
    }

    // Paste your MapTiler API key here (don't publish it in a public repository)
    private static OnlineTileSourceBase maptilerSource() {
        return new OnlineTileSourceBase("MapTiler-streets-v2", 0, 19, 512, ".png",
                new String[]{"https://api.maptiler.com/maps/streets-v2/"},
                "© MapTiler © OpenStreetMap contributors") {
            @Override public String getTileURLString(long idx) {
                return getBaseUrl() + MapTileIndex.getZoom(idx) + "/" + MapTileIndex.getX(idx)
                        + "/" + MapTileIndex.getY(idx) + ".png?key=" + MAPTILER_KEY;
            }
        };
    }
    private static double parse(String s) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return Double.NaN; }
    }

    /** Show a pin and the details for a chosen point. */
    private void showSelection(double lat, double lon, String name) {
        selLat = lat;
        selLon = lon;
        selName = name == null ? "" : name;
        if (marker == null) {
            marker = new Marker(map);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            map.getOverlays().add(marker);
        }
        marker.setPosition(new GeoPoint(lat, lon));
        map.invalidate();
        tvSelected.setText(String.format(Locale.US, "%s\n%.4f, %.4f", selName, lat, lon));
    }

    /** A tap on the map: show the pin now, then look up its name in the background. */
    private void select(final double lat, final double lon) {
        showSelection(lat, lon, "Looking up place…");
        final Context app = requireContext().getApplicationContext();
        new Thread(() -> {
            final String name = reverseName(app, lat, lon);
            runUi(() -> {
                if (selLat == lat && selLon == lon) showSelection(lat, lon, name);
            });
        }).start();
    }

    private static String reverseName(Context c, double lat, double lon) {
        try {
            if (Geocoder.isPresent()) {
                List<Address> a = new Geocoder(c, Locale.getDefault()).getFromLocation(lat, lon, 1);
                if (a != null && !a.isEmpty()) {
                    Address ad = a.get(0);
                    String n = ad.getLocality();
                    if (n == null) n = ad.getSubAdminArea();
                    if (n == null) n = ad.getAdminArea();
                    if (n == null) n = ad.getCountryName();
                    if (n != null) return n;
                }
            }
        } catch (Exception ignored) { }
        return String.format(Locale.US, "%.3f, %.3f", lat, lon);
    }

    private void search() {
        final String q = etSearch.getText().toString().trim();
        if (q.isEmpty()) return;
        tvSelected.setText("Searching…");
        new Thread(() -> {
            List<GeoClient.Place> res;
            String err = null;
            try {
                res = GeoClient.search(q);
            } catch (Exception e) {
                res = new ArrayList<>();
                err = e.getMessage();
            }
            final List<GeoClient.Place> found = res;
            final String error = err;
            runUi(() -> {
                if (error != null) { tvSelected.setText("Search failed: " + error); return; }
                if (found.isEmpty()) { tvSelected.setText("No place found for \"" + q + "\""); return; }
                if (found.size() == 1) { pick(found.get(0)); return; }
                String[] labels = new String[found.size()];
                for (int i = 0; i < labels.length; i++) labels[i] = found.get(i).label;
                new AlertDialog.Builder(requireContext())
                        .setTitle("Choose a place")
                        .setItems(labels, (d, i) -> pick(found.get(i)))
                        .show();
            });
        }).start();
    }

    private void pick(GeoClient.Place p) {
        map.getController().setZoom(10.0);
        map.getController().animateTo(new GeoPoint(p.lat, p.lon));
        showSelection(p.lat, p.lon, p.name);
    }

    private void useLocation() {
        if (Double.isNaN(selLat) || Double.isNaN(selLon)) {
            Toast.makeText(requireContext(), "Tap the map to choose a location first", Toast.LENGTH_SHORT).show();
            return;
        }
        Context app = requireContext().getApplicationContext();
        String name = selName.isEmpty() || selName.startsWith("Looking") ? "Selected location" : selName;
        AppPrefs.saveLocation(app, String.valueOf(selLat), String.valueOf(selLon), name);
        new ViewModelProvider(requireActivity()).get(OutdoorViewModel.class).refreshNow();
        Toast.makeText(requireContext(), "Location set: " + name, Toast.LENGTH_SHORT).show();
        ((MainActivity) requireActivity()).selectTab(R.id.nav_dashboard);
    }

    private void runUi(Runnable r) {
        if (getActivity() != null) getActivity().runOnUiThread(() -> { if (isAdded()) r.run(); });
    }

    @Override public void onResume() {
        super.onResume();
        if (map != null) map.onResume();
    }

    @Override public void onPause() {
        if (map != null) map.onPause();
        super.onPause();
    }

    @Override public void onDestroyView() {
        if (map != null) { map.onDetach(); map = null; }
        marker = null;
        super.onDestroyView();
    }
}
