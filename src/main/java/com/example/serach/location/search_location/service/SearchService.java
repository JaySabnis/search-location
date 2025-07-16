package com.example.serach.location.search_location.service;

import com.example.serach.location.search_location.dto.PlaceInfoDTO;
import com.example.serach.location.search_location.dto.PolygonRequestDTO;
import com.example.serach.location.search_location.entity.Place;
import com.example.serach.location.search_location.repository.PlacesRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private final PlacesRepository placesRepository;

    @Autowired
    public SearchService(MongoTemplate mongoTemplate, PlacesRepository placesRepository) {
        this.placesRepository = placesRepository;
    }

    public List<String> getListOfCategories(){
        return placesRepository.getAllCategories();
    }

    public List<PlaceInfoDTO> getPlacesWithinRadius(double centerLat, double centerLng, double radiusMeter, String category) {
        List<Place> results = placesRepository.findNearby(centerLng, centerLat, radiusMeter, category);
        return buildResponseListOfPlaces(results);
    }

    public List<PlaceInfoDTO> findPlacesWithinPolygon(PolygonRequestDTO polygonRequestDTO) {
        List<Place> results = placesRepository.findPlacesWithinPolygon(polygonRequestDTO);
        return buildResponseListOfPlaces(results);
    }

    private List<PlaceInfoDTO> buildResponseListOfPlaces(List<Place> places) {
        return places.stream().map(place -> {
            String name = place.getName();
            String category = place.getCategory();
            List<Double> coordinates = Arrays.asList(place.getLocation().getX(), place.getLocation().getY()); // [lng, lat]
            return new PlaceInfoDTO(name, category, coordinates);
        }).collect(Collectors.toList());
    }


}

