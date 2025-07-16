package com.example.serach.location.search_location;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class SearchLocationApplication {

	public static void main(String[] args) {
		SpringApplication.run(SearchLocationApplication.class, args);
	}

}
