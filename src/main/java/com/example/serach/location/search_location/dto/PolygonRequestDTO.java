package com.example.serach.location.search_location.dto;

import lombok.Data;

import java.util.List;

@Data
public class PolygonRequestDTO {
    private String category;
    private List<List<List<Double>>> polygon;
}
