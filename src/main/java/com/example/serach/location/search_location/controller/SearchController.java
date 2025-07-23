package com.example.serach.location.search_location.controller;

import com.example.serach.location.search_location.dto.PlaceInfoDTO;
import com.example.serach.location.search_location.dto.PlaceInfoResponseDTO;
import com.example.serach.location.search_location.dto.PolygonRequestDTO;
import com.example.serach.location.search_location.entity.Place;
import com.example.serach.location.search_location.service.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/v1/search")
public class SearchController {

    @Autowired
    private SearchService searchService;

    @GetMapping("/coordinates")
    public PlaceInfoResponseDTO getCitiesWithinRadius(@RequestParam Double centerLat, @RequestParam Double centerLng, @RequestParam Double radiusMeter , @RequestParam(required = false) String category,  @RequestParam(required = false, defaultValue = "0") int offset, @RequestParam(required = false, defaultValue ="20") int limit){
        log.info("Request to search location for coordinates({},{}) within radius : {}",centerLat,centerLng,radiusMeter);
        return  searchService.getPlacesWithinRadius(centerLat,centerLng,radiusMeter,category,offset,limit);
    }

    @PostMapping("/within-polygon")
    public ResponseEntity<PlaceInfoResponseDTO> findPlacesWithinPolygon(@RequestBody PolygonRequestDTO polygonRequest, @RequestParam(required = false, defaultValue = "0") int offset, @RequestParam(required = false, defaultValue ="20") int limit) {
        log.info("Request to search location for polygon : {} ",polygonRequest);
        PlaceInfoResponseDTO places=searchService.findPlacesWithinPolygon(polygonRequest,offset,limit);
        return ResponseEntity.ok(places);
    }

    @GetMapping("/list/category")
    public List<String> getListOfCategories(){
        return searchService.getListOfCategories();
    }
}
