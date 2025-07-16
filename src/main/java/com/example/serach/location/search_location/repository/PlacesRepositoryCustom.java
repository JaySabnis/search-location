package com.example.serach.location.search_location.repository;

import com.example.serach.location.search_location.dto.PolygonRequestDTO;
import com.example.serach.location.search_location.entity.Place;

import java.util.List;

public interface PlacesRepositoryCustom {
    List<Place> findPlacesWithinPolygon(PolygonRequestDTO polygonRequestDTO);
    List<Place> findNearby(double longitude, double latitude, double maxDistanceInMeters,String category);
    List<String> getAllCategories();
}
