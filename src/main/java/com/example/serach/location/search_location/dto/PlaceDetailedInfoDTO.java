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
        this.imageUrl = getImageUrlForCategory(place.getCategory());
    }

    private String getImageUrlForCategory(String category) {
        return switch (category) {
            case "Building" -> "https://drive.google.com/file/d/15yPdkOcQastmwOYUtU66QFgtivKJKAwM/view?usp=drive_link";
            case "Garden" -> "https://drive.google.com/file/d/1WesZKASh_eCjOcnrUZnaCgowNmZoieWu/view?usp=drive_link";
            case "Historical Site" -> "https://drive.google.com/file/d/1Qw5Hj5uSIMuAMwdu7p9nySnYqBnxWBeN/view?usp=drive_link";
            case "Hospital" -> "https://drive.google.com/file/d/1lwiaYzWU4hdwCeDOKZv81CoJahjUBt3B/view?usp=drive_link";
            case "Mall" -> "https://drive.google.com/file/d/1B-NcjRYWfwvKYfKMDNAKDhx7Ld1Vj7PQ/view?usp=drive_link";
            case "Market" -> "https://drive.google.com/file/d/1fnA1cfoyMfmKCPH1WKCgwyy_UwptB7-g/view?usp=drive_link";
            case "Museum" -> "https://drive.google.com/file/d/1YWKpCq2QNeHJtU_s0V6lSoRZ5YK0a1xZ/view?usp=drive_link";
            case "School" -> "https://drive.google.com/file/d/1w5DlelaPXWWp-ho_KoP-0_QczgfCzltA/view?usp=drive_link";
            case "Temple" -> "https://drive.google.com/file/d/1deUv7qUAtE4330j0USNKdTU6d0YI47jC/view?usp=drive_link";
            case "Theatre" -> "https://drive.google.com/file/d/1PuNYnmyipqD0peuyfdY_o3BG7rFyi9V0/view?usp=drive_link";
            default -> "https://cdn-icons-png.flaticon.com/64/235/235861.png";
        };
    }
}
