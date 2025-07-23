package com.example.serach.location.search_location.dto;

import com.example.serach.location.search_location.entity.Place;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.util.List;

@Data
public class PlaceDetailedInfoDTO {
    private String name;
    private String category;
    private String imageUrl;
//    private String description;
    private List<Double> coordinates;

    public PlaceDetailedInfoDTO(Place place) {
        BeanUtils.copyProperties(place,this);
        this.coordinates=place.getLocation().getCoordinates();
    }
}
