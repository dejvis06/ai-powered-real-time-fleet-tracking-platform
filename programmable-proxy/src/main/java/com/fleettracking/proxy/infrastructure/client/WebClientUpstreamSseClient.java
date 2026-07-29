package com.fleettracking.proxy.infrastructure.client;

import com.fleettracking.proxy.application.port.UpstreamSseClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.UUID;

/**
 * Subscribes to SSE streams from eta-service and delivery-service using WebClient.
 * Implements automatic reconnection with backoff.
 */
@Component
public class WebClientUpstreamSseClient implements UpstreamSseClient {

    private static final Logger log = LoggerFactory.getLogger(WebClientUpstreamSseClient.class);

    private final WebClient etaWebClient;
    private final WebClient deliveryWebClient;

    public WebClientUpstreamSseClient(
            WebClient.Builder webClientBuilder,
            @Value("${proxy.eta-service-url:http://localhost:8083}") String etaServiceUrl,
            @Value("${proxy.delivery-service-url:http://localhost:8084}") String deliveryServiceUrl
    ) {
        this.etaWebClient = webClientBuilder.baseUrl(etaServiceUrl).build();
        this.deliveryWebClient = webClientBuilder.baseUrl(deliveryServiceUrl).build();
    }

    @Override
    public Flux<ServerSentEvent<String>> subscribeEtaStream(UUID deliveryId) {
        return etaWebClient.get()
                .uri("/deliveries/{deliveryId}/sse", deliveryId)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .retryWhen(Retry.backoff(5, Duration.ofSeconds(1))
                        .maxBackoff(Duration.ofSeconds(30))
                        .doBeforeRetry(signal ->
                                log.warn("Retrying eta-service SSE connection for delivery={} attempt={}",
                                        deliveryId, signal.totalRetries())))
                .doOnError(e -> log.error("Lost connection to eta-service for delivery={}", deliveryId, e));
    }

    @Override
    public Flux<ServerSentEvent<String>> subscribeDeliveryStream(UUID deliveryId) {
        return deliveryWebClient.get()
                .uri("/deliveries/{deliveryId}/sse", deliveryId)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .retryWhen(Retry.backoff(5, Duration.ofSeconds(1))
                        .maxBackoff(Duration.ofSeconds(30))
                        .doBeforeRetry(signal ->
                                log.warn("Retrying delivery-service SSE connection for delivery={} attempt={}",
                                        deliveryId, signal.totalRetries())))
                .doOnError(e -> log.error("Lost connection to delivery-service for delivery={}", deliveryId, e));
    }
}
