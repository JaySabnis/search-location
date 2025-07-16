package com.example.serach.location.search_location.repository;

import com.example.serach.location.search_location.entity.Place;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface PlacesRepository  extends MongoRepository<Place,String>,PlacesRepositoryCustom{

//    @Query(value = "{}", fields = "{ 'category' : 1 }")
//    List<Place> findAllCategoriesRaw();

}
