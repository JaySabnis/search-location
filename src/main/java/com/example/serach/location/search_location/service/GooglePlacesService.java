package com.example.serach.location.search_location.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GooglePlacesService {

    private static final String GOOGLE_API_KEY = System.getenv("GOOGLE_API_KEY");

    public static List<Map<String, Object>> getFoodPlaces(String location, String cuisine, int radius) throws IOException {
        String url = "https://maps.googleapis.com/maps/api/place/textsearch/json";
        String query = URLEncoder.encode(cuisine + " restaurants in " + location, StandardCharsets.UTF_8);
        String requestUrl = String.format("%s?query=%s&radius=%d&key=%s", url, query, radius, GOOGLE_API_KEY);

        HttpGet request = new HttpGet(requestUrl);
        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(request)) {

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getEntity().getContent());

            List<Map<String, Object>> places = new ArrayList<>();
            for (JsonNode place : root.path("results")) {
                String name = place.path("name").asText();
                String address = place.path("formatted_address").asText();
                double rating = place.path("rating").asDouble();

                // Coordinates
                JsonNode locationNode = place.path("geometry").path("location");
                double lat = locationNode.path("lat").asDouble();
                double lng = locationNode.path("lng").asDouble();

                // Image URL using photo_reference (if available)
                String imageUrl = "";
                JsonNode photos = place.path("photos");
                if (photos.isArray() && photos.size() > 0) {
                    String photoRef = photos.get(0).path("photo_reference").asText();
                    imageUrl = "https://maps.googleapis.com/maps/api/place/photo" +
                            "?maxwidth=400" +
                            "&photo_reference=" + photoRef +
                            "&key=" + GOOGLE_API_KEY;
                }

                Map<String, Object> placeData = Map.of(
                        "name", name,
                        "address", address,
                        "rating", rating,
                        "lat", lat,
                        "lng", lng,
                        "imageUrl", imageUrl
                );

                places.add(placeData);
                if (places.size() == 5) break; // Limit to top 5
            }

            return places;
        }
    }
}

