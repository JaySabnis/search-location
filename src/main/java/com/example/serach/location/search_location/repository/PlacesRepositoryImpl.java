package com.example.serach.location.search_location.repository;

import com.example.serach.location.search_location.dto.PolygonRequestDTO;
import com.example.serach.location.search_location.entity.Place;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPolygon;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class PlacesRepositoryImpl implements PlacesRepositoryCustom{

    private final MongoTemplate mongoTemplate;

    public PlacesRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<Place> findPlacesWithinPolygon(PolygonRequestDTO polygonRequestDTO) {
        List<Point> points = polygonRequestDTO.getPolygon().get(0).stream()
                .map(coord -> new Point(coord.get(0), coord.get(1)))
                .collect(Collectors.toList());

        GeoJsonPolygon polygon = new GeoJsonPolygon(points);

        Criteria geoCriteria = Criteria.where("location").within(polygon);

        Query query = new Query();
        if (polygonRequestDTO.getCategory() != null) {
            query.addCriteria(Criteria.where("category").is(polygonRequestDTO.getCategory()));
        }
        query.addCriteria(geoCriteria);

        return mongoTemplate.find(query, Place.class);
    }

    @Override
    public List<Place> findNearby(double longitude, double latitude, double maxDistanceInMeters,String category) {
        Point location = new Point(longitude, latitude);

        Criteria geoCriteria = Criteria.where("location")
                .nearSphere(location)
                .maxDistance(maxDistanceInMeters);

        Criteria finalCriteria = geoCriteria;

        if (category != null && !category.isEmpty()) {
            finalCriteria = new Criteria().andOperator(
                    geoCriteria,
                    Criteria.where("category").is(category)
            );
        }

        Query query = new Query(finalCriteria);

        return mongoTemplate.find(query, Place.class);
    }

    public List<String> getAllCategories() {
        return mongoTemplate.query(Place.class)
                .distinct("category")
                .as(String.class)
                .all();
    }


}
