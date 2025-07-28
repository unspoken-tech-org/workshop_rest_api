package com.tproject.workshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class WorkshopApplication {

	public static void main(String[] args) {
		SpringApplication.run(WorkshopApplication.class, args);
	}

}
