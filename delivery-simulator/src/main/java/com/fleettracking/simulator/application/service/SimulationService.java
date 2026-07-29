package com.fleettracking.simulator.application.service;

import com.fleettracking.simulator.application.port.DeliveryServiceClient;
import com.fleettracking.simulator.application.port.LocationPublisher;
import com.fleettracking.simulator.domain.model.SimulatedDelivery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Orchestrates delivery simulations.
 *
 * Workflow per delivery:
 *  1. POST /deliveries → create delivery (route comes from delivery-service)
 *  2. POST /deliveries/{id}/start → transition to ACTIVE
 *  3. Every N seconds: publish MQTT location for current waypoint → advance
 *  4. POST /deliveries/{id}/complete → when all waypoints visited
 */
@Service
public class SimulationService {

    private static final Logger log = LoggerFactory.getLogger(SimulationService.class);

    private final DeliveryServiceClient deliveryServiceClient;
    private final LocationPublisher locationPublisher;

    @Value("${simulator.location-publish-interval-ms:3000}")
    private long locationPublishIntervalMs;

    @Value("${simulator.speed-kph:50.0}")
    private double speedKph;

    // Active simulations keyed by deliveryId
    private final ConcurrentHashMap<UUID, SimulatedDelivery> activeSimulations = new ConcurrentHashMap<>();

    public SimulationService(
            DeliveryServiceClient deliveryServiceClient,
            LocationPublisher locationPublisher
    ) {
        this.deliveryServiceClient = deliveryServiceClient;
        this.locationPublisher = locationPublisher;
    }

    /**
     * Starts a new delivery simulation.
     */
    public UUID startSimulation(
            double originLat, double originLon,
            double destLat, double destLon
    ) {
        UUID vehicleId = UUID.randomUUID();

        // Create delivery via REST
        DeliveryServiceClient.CreateDeliveryResult result = deliveryServiceClient.createDelivery(
                vehicleId, originLat, originLon, destLat, destLon);

        List<SimulatedDelivery.Waypoint> waypoints = result.waypoints().stream()
                .map(w -> new SimulatedDelivery.Waypoint(w.latitude(), w.longitude()))
                .toList();

        // If no waypoints from route API, use origin and destination directly
        if (waypoints.isEmpty()) {
            waypoints = List.of(
                    new SimulatedDelivery.Waypoint(originLat, originLon),
                    new SimulatedDelivery.Waypoint(destLat, destLon)
            );
        }

        SimulatedDelivery simulation = new SimulatedDelivery(result.deliveryId(), vehicleId, waypoints);
        simulation.setStatus(SimulatedDelivery.SimulationStatus.PENDING);

        // Start delivery in delivery-service
        deliveryServiceClient.startDelivery(result.deliveryId());
        simulation.setStatus(SimulatedDelivery.SimulationStatus.RUNNING);

        activeSimulations.put(result.deliveryId(), simulation);
        log.info("Started simulation for delivery={} vehicle={}", result.deliveryId(), vehicleId);

        return result.deliveryId();
    }

    /**
     * Scheduled tick: advances each active simulation by one waypoint and publishes location.
     */
    @Scheduled(fixedDelayString = "${simulator.location-publish-interval-ms:3000}")
    public void tick() {
        for (SimulatedDelivery simulation : activeSimulations.values()) {
            if (simulation.getStatus() != SimulatedDelivery.SimulationStatus.RUNNING) continue;
            tickSimulation(simulation);
        }
    }

    private void tickSimulation(SimulatedDelivery simulation) {
        try {
            if (!simulation.hasMoreWaypoints()) {
                completeSimulation(simulation);
                return;
            }

            SimulatedDelivery.Waypoint waypoint = simulation.currentWaypoint();
            double heading = simulation.headingToNext();

            locationPublisher.publishLocation(
                    simulation.getVehicleId(),
                    simulation.getDeliveryId(),
                    waypoint.latitude(),
                    waypoint.longitude(),
                    heading,
                    speedKph
            );

            simulation.advanceToNextWaypoint();

        } catch (Exception e) {
            log.error("Error during simulation tick for delivery={}", simulation.getDeliveryId(), e);
        }
    }

    private void completeSimulation(SimulatedDelivery simulation) {
        simulation.setStatus(SimulatedDelivery.SimulationStatus.COMPLETED);
        activeSimulations.remove(simulation.getDeliveryId());
        try {
            deliveryServiceClient.completeDelivery(simulation.getDeliveryId());
            log.info("Completed simulation for delivery={}", simulation.getDeliveryId());
        } catch (Exception e) {
            log.error("Failed to complete delivery={}", simulation.getDeliveryId(), e);
        }
    }
}
