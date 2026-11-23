package com.example.airquality;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

public class SettingsFragment extends Fragment {

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle s) {
        View v = inflater.inflate(R.layout.fragment_settings, container, false);
        EditText etToken = v.findViewById(R.id.etToken);
        EditText etServer = v.findViewById(R.id.etServer);
        TextView tvLocation = v.findViewById(R.id.tvLocation);
        Button btnTest = v.findViewById(R.id.btnTest);
        Button btnOutdoor = v.findViewById(R.id.btnTestOutdoor);
        Button btnSave = v.findViewById(R.id.btnSave);
        Button btnMap = v.findViewById(R.id.btnMap);
        TextView tvResult = v.findViewById(R.id.tvResult);

        etToken.setText(AppPrefs.getToken(requireContext()));
        etServer.setText(AppPrefs.getServer(requireContext()));

        final String lat = AppPrefs.getLat(requireContext());
        final String lon = AppPrefs.getLon(requireContext());
        tvLocation.setText(lat.isEmpty() || lon.isEmpty() ? "Location: not set"
                : "Location: " + AppPrefs.getPlace(requireContext()) + " (" + lat + ", " + lon + ")");

        btnMap.setOnClickListener(x -> {
            requireActivity().getSupportFragmentManager().popBackStack();
            ((MainActivity) requireActivity()).selectTab(R.id.nav_map);
        });

        btnSave.setOnClickListener(x -> {
            AppPrefs.save(requireContext(), etToken.getText().toString().trim(),
                    etServer.getText().toString().trim());
            ViewModelProvider p = new ViewModelProvider(requireActivity());
            p.get(ReadingViewModel.class).refreshNow();
            p.get(OutdoorViewModel.class).refreshNow();
            Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show();
        });

        btnTest.setOnClickListener(x -> {
            tvResult.setText("Testing…");
            final String token = etToken.getText().toString().trim();
            final String server = etServer.getText().toString().trim();
            new Thread(() -> {
                BlynkClient.Result r = BlynkClient.fetch(server, token);
                String msg = r.error != null ? "✗ " + r.error
                        : "✓ Device " + (r.deviceOnline ? "online" : "offline")
                        + "\nGas index (V0): " + Param.GAS.format(r.reading.gas) + " ppm-eq";
                post(tvResult, msg);
            }).start();
        });

        btnOutdoor.setOnClickListener(x -> {
            tvResult.setText("Testing outdoor API…");
            new Thread(() -> {
                String msg;
                try {
                    OpenMeteoClient.Result r = OpenMeteoClient.fetch(
                            Double.parseDouble(lat.trim()), Double.parseDouble(lon.trim()));
                    msg = r.error != null ? "✗ " + r.error
                            : "✓ Outdoor: " + r.data.temp + " °C, " + r.data.humidity + " %\n"
                            + "PM2.5: " + r.data.pm25 + " µg/m³, US AQI: " + r.data.usAqi;
                } catch (NumberFormatException e) {
                    msg = "✗ Choose your location on the Map tab first";
                }
                post(tvResult, msg);
            }).start();
        });
        return v;
    }

    private void post(TextView tv, String msg) {
        final String out = msg;
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> { if (isAdded()) tv.setText(out); });
        }
    }
}
