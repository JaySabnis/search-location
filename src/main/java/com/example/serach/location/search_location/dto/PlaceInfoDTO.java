package com.example.serach.location.search_location.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PlaceInfoDTO {

    private String name;
    private String category;
    private List<Double> coordinates;

}
