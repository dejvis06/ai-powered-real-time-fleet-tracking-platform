package com.fleettracking.simulator.infrastructure.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fleettracking.simulator.application.port.DeliveryServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class HttpDeliveryServiceClient implements DeliveryServiceClient {

    private static final Logger log = LoggerFactory.getLogger(HttpDeliveryServiceClient.class);

    private final RestClient restClient;

    public HttpDeliveryServiceClient(
            RestClient.Builder builder,
            @Value("${simulator.delivery-service-url:http://localhost:8081}") String deliveryServiceUrl
    ) {
        this.restClient = builder.baseUrl(deliveryServiceUrl).build();
    }

    @Override
    public CreateDeliveryResult createDelivery(
            UUID vehicleId,
            double originLat, double originLon,
            double destLat, double destLon
    ) {
        Map<String, Object> body = Map.of(
                "vehicleId", vehicleId.toString(),
                "originLatitude", originLat,
                "originLongitude", originLon,
                "destinationLatitude", destLat,
                "destinationLongitude", destLon
        );

        JsonNode response = restClient.post()
                .uri("/deliveries")
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        UUID deliveryId = UUID.fromString(response.path("deliveryId").asText());
        List<Waypoint> waypoints = new ArrayList<>();
        JsonNode waypointsNode = response.path("waypoints");
        if (waypointsNode.isArray()) {
            for (JsonNode wp : waypointsNode) {
                waypoints.add(new Waypoint(
                        wp.path("latitude").asDouble(),
                        wp.path("longitude").asDouble()
                ));
            }
        }

        log.info("Created delivery={} with {} waypoints", deliveryId, waypoints.size());
        return new CreateDeliveryResult(deliveryId, waypoints);
    }

    @Override
    public void startDelivery(UUID deliveryId) {
        restClient.post()
                .uri("/deliveries/{id}/start", deliveryId)
                .retrieve()
                .toBodilessEntity();
        log.info("Started delivery={}", deliveryId);
    }

    @Override
    public void completeDelivery(UUID deliveryId) {
        restClient.post()
                .uri("/deliveries/{id}/complete", deliveryId)
                .retrieve()
                .toBodilessEntity();
        log.info("Completed delivery={}", deliveryId);
    }

    @Override
    public void failDelivery(UUID deliveryId, String reason) {
        restClient.post()
                .uri("/deliveries/{id}/fail", deliveryId)
                .body(Map.of("reason", reason))
                .retrieve()
                .toBodilessEntity();
        log.info("Failed delivery={} reason={}", deliveryId, reason);
    }
}
