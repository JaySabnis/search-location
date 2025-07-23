package com.example.serach.location.search_location.dto;

import lombok.Data;

import java.util.List;

@Data
public class PlaceInfoResponseDTO {
    private List<PlaceDetailedInfoDTO> detailedPlaces;
    private List<PlaceInfoDTO> allCoordinates;
}
