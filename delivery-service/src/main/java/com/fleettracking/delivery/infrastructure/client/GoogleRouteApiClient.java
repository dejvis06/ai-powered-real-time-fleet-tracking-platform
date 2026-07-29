package com.fleettracking.delivery.infrastructure.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fleettracking.delivery.application.port.RouteApiClient;
import com.fleettracking.delivery.domain.model.GeoPoint;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

/**
 * HTTP client for Google Routes API v2.
 * Calls computeRoutes to get an encoded polyline and duration/distance.
 */
@Component
public class GoogleRouteApiClient implements RouteApiClient {

    private static final Logger log = LoggerFactory.getLogger(GoogleRouteApiClient.class);

    private static final String ROUTES_API_URL =
            "https://routes.googleapis.com/directions/v2:computeRoutes";

    private final RestClient restClient;
    private final String apiKey;
    private final MeterRegistry meterRegistry;

    public GoogleRouteApiClient(
            RestClient.Builder restClientBuilder,
            @Value("${google.routes.api-key}") String apiKey,
            MeterRegistry meterRegistry
    ) {
        this.restClient = restClientBuilder.baseUrl(ROUTES_API_URL).build();
        this.apiKey = apiKey;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public RouteResult computeRoute(GeoPoint origin, GeoPoint destination, List<GeoPoint> waypoints) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            String requestBody = buildRequestBody(origin, destination, waypoints);

            JsonNode response = restClient.post()
                    .header("X-Goog-Api-Key", apiKey)
                    .header("X-Goog-FieldMask",
                            "routes.polyline.encodedPolyline,routes.distanceMeters,routes.duration,routes.legs")
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .retrieve()
                    .body(JsonNode.class);

            meterRegistry.counter("google.routes.requests").increment();

            return parseResponse(response);

        } catch (Exception e) {
            meterRegistry.counter("google.routes.errors").increment();
            log.error("Google Routes API call failed", e);
            throw new RouteApiException("Failed to compute route via Google Routes API", e);
        } finally {
            sample.stop(meterRegistry.timer("google.routes.latency"));
        }
    }

    private String buildRequestBody(GeoPoint origin, GeoPoint destination, List<GeoPoint> waypoints) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                {
                  "origin": { "location": { "latLng": { "latitude": %s, "longitude": %s } } },
                  "destination": { "location": { "latLng": { "latitude": %s, "longitude": %s } } },
                """.formatted(origin.latitude(), origin.longitude(),
                destination.latitude(), destination.longitude()));

        if (!waypoints.isEmpty()) {
            sb.append("\"intermediates\": [");
            for (int i = 0; i < waypoints.size(); i++) {
                GeoPoint wp = waypoints.get(i);
                sb.append("""
                        { "location": { "latLng": { "latitude": %s, "longitude": %s } } }
                        """.formatted(wp.latitude(), wp.longitude()));
                if (i < waypoints.size() - 1) sb.append(",");
            }
            sb.append("],");
        }

        sb.append("""
                  "travelMode": "DRIVE",
                  "routingPreference": "TRAFFIC_AWARE",
                  "computeAlternativeRoutes": false,
                  "routeModifiers": { "avoidTolls": false, "avoidHighways": false }
                }
                """);

        return sb.toString();
    }

    private RouteResult parseResponse(JsonNode response) {
        JsonNode route = response.path("routes").get(0);
        String encodedPolyline = route.path("polyline").path("encodedPolyline").asText();
        long distanceMeters = route.path("distanceMeters").asLong();
        String durationStr = route.path("duration").asText("0s");
        long durationSeconds = parseDurationSeconds(durationStr);

        // Extract leg waypoints for simulation use
        List<GeoPoint> decoded = new ArrayList<>();
        JsonNode legs = route.path("legs");
        if (legs.isArray()) {
            for (JsonNode leg : legs) {
                JsonNode steps = leg.path("steps");
                if (steps.isArray()) {
                    for (JsonNode step : steps) {
                        JsonNode startLoc = step.path("startLocation").path("latLng");
                        if (!startLoc.isMissingNode()) {
                            decoded.add(new GeoPoint(
                                    startLoc.path("latitude").asDouble(),
                                    startLoc.path("longitude").asDouble()
                            ));
                        }
                    }
                }
            }
        }

        return new RouteResult(encodedPolyline, decoded, distanceMeters, durationSeconds);
    }

    private long parseDurationSeconds(String duration) {
        // format: "123s"
        if (duration.endsWith("s")) {
            try {
                return Long.parseLong(duration.substring(0, duration.length() - 1));
            } catch (NumberFormatException ignored) {}
        }
        return 0L;
    }

    public static class RouteApiException extends RuntimeException {
        public RouteApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
