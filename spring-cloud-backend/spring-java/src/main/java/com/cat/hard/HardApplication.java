package com.cat.hard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class HardApplication {

	public static void main(String[] args) {
		SpringApplication.run(HardApplication.class, args);
	}
}
