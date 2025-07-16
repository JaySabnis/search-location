package com.example.serach.location.search_location.entity;

import jakarta.persistence.Id;
import lombok.Data;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "places")
public class Place {
    @Id
    private String id;
    private String name;
    private String category;

    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)//Also create index in db.
    private GeoJsonPoint location;
}
