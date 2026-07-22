package com.ingestion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MqttIngestionServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(MqttIngestionServiceApplication.class, args);
	}

}
