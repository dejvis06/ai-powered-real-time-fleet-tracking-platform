package com.fleettracking.eta.infrastructure.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fleettracking.eta.application.port.RoutesApiPort;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Reactive adapter for Google Routes API v2.
 */
@Component
public class GoogleRoutesApiAdapter implements RoutesApiPort {

    private static final Logger log = LoggerFactory.getLogger(GoogleRoutesApiAdapter.class);

    private final WebClient webClient;
    private final String apiKey;
    private final MeterRegistry meterRegistry;

    public GoogleRoutesApiAdapter(
            WebClient.Builder webClientBuilder,
            @Value("${google.routes.api-key}") String apiKey,
            MeterRegistry meterRegistry
    ) {
        this.webClient = webClientBuilder
                .baseUrl("https://routes.googleapis.com")
                .build();
        this.apiKey = apiKey;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public Mono<RouteResult> computeRoute(
            double originLat, double originLon,
            double destLat, double destLon
    ) {
        String body = """
                {
                  "origin": { "location": { "latLng": { "latitude": %s, "longitude": %s } } },
                  "destination": { "location": { "latLng": { "latitude": %s, "longitude": %s } } },
                  "travelMode": "DRIVE",
                  "routingPreference": "TRAFFIC_AWARE",
                  "computeAlternativeRoutes": false
                }
                """.formatted(originLat, originLon, destLat, destLon);

        return webClient.post()
                .uri("/directions/v2:computeRoutes")
                .header("X-Goog-Api-Key", apiKey)
                .header("X-Goog-FieldMask", "routes.distanceMeters,routes.duration")
                .header("Content-Type", "application/json")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(this::parseResult)
                .doOnNext(r -> meterRegistry.counter("google.routes.requests").increment())
                .doOnError(e -> {
                    log.error("Google Routes API call failed", e);
                    meterRegistry.counter("google.routes.errors").increment();
                });
    }

    private RouteResult parseResult(JsonNode response) {
        JsonNode route = response.path("routes").get(0);
        long distanceMeters = route.path("distanceMeters").asLong();
        String durationStr = route.path("duration").asText("0s");
        long durationSeconds = parseDurationSeconds(durationStr);
        return new RouteResult(distanceMeters, durationSeconds);
    }

    private long parseDurationSeconds(String duration) {
        if (duration != null && duration.endsWith("s")) {
            try {
                return Long.parseLong(duration.substring(0, duration.length() - 1));
            } catch (NumberFormatException ignored) {}
        }
        return 0L;
    }
}
