package com.fleettracking.simulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DeliverySimulatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeliverySimulatorApplication.class, args);
    }
}
