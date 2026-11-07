package com.tproject.workshop;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

// Authentication is JWT/API key; the default user would only log a generated password.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableAsync
@ConfigurationPropertiesScan
public class WorkshopApplication {

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
		SpringApplication.run(WorkshopApplication.class, args);
	}

}
