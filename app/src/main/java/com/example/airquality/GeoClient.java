
package com.example.airquality;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Place search using Open-Meteo's free Geocoding API.
 * Must be called from a background thread.
 */
public class GeoClient {

    public static class Place {
        public String name;
        public String label;
        public double lat;
        public double lon;
    }

    public static List<Place> search(String query) throws Exception {

        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String encodedQuery = URLEncoder.encode(
                query.trim(),
                "UTF-8"
        );

        String url =
                "https://geocoding-api.open-meteo.com/v1/search"
                        + "?name=" + encodedQuery
                        + "&count=5"
                        + "&language=en"
                        + "&format=json";

        // Execute HTTP request
        JSONObject response = new JSONObject(
                Http.get(url)
        );

        JSONArray results = response.optJSONArray("results");

        List<Place> places = new ArrayList<>();

        if (results == null) {
            return places;
        }

        for (int i = 0; i < results.length(); i++) {

            JSONObject result = results.getJSONObject(i);

            Place place = new Place();

            place.name = result.optString("name", "");

            place.lat = result.optDouble("latitude", 0.0);

            place.lon = result.optDouble("longitude", 0.0);

            String admin = result.optString("admin1", "");

            String country = result.optString("country", "");

            place.label = place.name
                    + (admin.isEmpty() ? "" : ", " + admin)
                    + (country.isEmpty() ? "" : ", " + country);

            places.add(place);
        }

        return places;
    }
}
