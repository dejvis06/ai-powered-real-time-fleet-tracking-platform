package com.fleettracking.simulator.interfaces;

import com.fleettracking.simulator.application.service.SimulationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Exposes a REST endpoint to trigger a new delivery simulation.
 * Intended for development and testing use.
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/simulate")
public class SimulatorController {

    private final SimulationService simulationService;

    public SimulatorController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> startSimulation(@RequestBody SimulateRequest request) {
        UUID deliveryId = simulationService.startSimulation(
                request.originLat(), request.originLon(),
                request.destLat(), request.destLon()
        );
        return ResponseEntity.ok(Map.of("deliveryId", deliveryId.toString()));
    }

    public record SimulateRequest(
            double originLat,
            double originLon,
            double destLat,
            double destLon
    ) {}
}
