package com.example.serach.location.search_location.service;

import com.example.serach.location.search_location.dto.PlaceDetailedInfoDTO;
import com.example.serach.location.search_location.dto.PlaceInfoDTO;
import com.example.serach.location.search_location.dto.PlaceInfoResponseDTO;
import com.example.serach.location.search_location.dto.PolygonRequestDTO;
import com.example.serach.location.search_location.entity.Place;
import com.example.serach.location.search_location.repository.PlacesRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

    public PlaceInfoResponseDTO getPlacesWithinRadius(double centerLat, double centerLng, double radiusMeter, String category, int offset, int limit) {
        List<Place> results = placesRepository.findNearby(centerLng, centerLat, radiusMeter, category);

        // Slice detailed list using pagination
        int toIndex = Math.min(offset + limit, results.size());
        List<Place> detailedPlaces = (offset < toIndex) ? results.subList(offset, toIndex) : Collections.emptyList();

        // Convert detailed places
        List<PlaceDetailedInfoDTO> detailedDTOs = detailedPlaces.stream()
                .map(PlaceDetailedInfoDTO::new)
                .collect(Collectors.toList());

        // Convert full coordinate list
        List<PlaceInfoDTO> allCoords = results.stream()
                .map(PlaceInfoDTO::new)
                .collect(Collectors.toList());

        // Build response
        PlaceInfoResponseDTO response = new PlaceInfoResponseDTO();
        response.setDetailedPlaces(detailedDTOs);
        response.setAllCoordinates(allCoords);
        return response;
    }

    public PlaceInfoResponseDTO findPlacesWithinPolygon(PolygonRequestDTO polygonRequestDTO,int offset,int limit) {
        List<Place> results = placesRepository.findPlacesWithinPolygon(polygonRequestDTO);

        int toIndex = Math.min(offset + limit, results.size());
        List<Place> detailedPlaces = (offset < toIndex) ? results.subList(offset, toIndex) : Collections.emptyList();

        // Convert detailed places
        List<PlaceDetailedInfoDTO> detailedDTOs = detailedPlaces.stream()
                .map(PlaceDetailedInfoDTO::new)
                .collect(Collectors.toList());

        // Convert full coordinate list
        List<PlaceInfoDTO> allCoords = results.stream()
                .map(PlaceInfoDTO::new)
                .collect(Collectors.toList());

        // Build response
        PlaceInfoResponseDTO response = new PlaceInfoResponseDTO();
        response.setDetailedPlaces(detailedDTOs);
        response.setAllCoordinates(allCoords);
        return response;

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

