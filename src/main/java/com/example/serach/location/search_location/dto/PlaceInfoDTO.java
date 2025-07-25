package com.example.serach.location.search_location.dto;

import com.example.serach.location.search_location.entity.Place;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.util.List;

@Data
@AllArgsConstructor
public class PlaceInfoDTO {

    private String name;
    private String category;
    private List<Double> coordinates;

    public PlaceInfoDTO(Place place) {
        BeanUtils.copyProperties(place,this);
        this.coordinates=place.getLocation().getCoordinates();
    }
}
