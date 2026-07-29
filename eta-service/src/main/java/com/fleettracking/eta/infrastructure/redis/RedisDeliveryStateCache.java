package com.fleettracking.eta.infrastructure.redis;

import com.fleettracking.common.model.DeliveryStatus;
import com.fleettracking.eta.application.port.DeliveryStateCache;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class RedisDeliveryStateCache implements DeliveryStateCache {

    private static final Logger log = LoggerFactory.getLogger(RedisDeliveryStateCache.class);

    private final ReactiveStringRedisTemplate redisTemplate;
    private final MeterRegistry meterRegistry;

    public RedisDeliveryStateCache(
            ReactiveStringRedisTemplate redisTemplate,
            MeterRegistry meterRegistry
    ) {
        this.redisTemplate = redisTemplate;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public Mono<DeliveryStatus> getStatus(UUID deliveryId) {
        String key = "delivery:status:" + deliveryId;
        return redisTemplate.opsForValue().get(key)
                .map(value -> {
                    meterRegistry.counter("redis.routing.lookup").increment();
                    return DeliveryStatus.valueOf(value);
                })
                .switchIfEmpty(Mono.fromCallable(() -> {
                    log.warn("No delivery status in Redis for delivery={}", deliveryId);
                    meterRegistry.counter("redis.routing.miss").increment();
                    return DeliveryStatus.ACTIVE;
                }));
    }

    @Override
    public Mono<DeliveryDestination> getDestination(UUID deliveryId) {
        String latKey = "delivery:destination:lat:" + deliveryId;
        String lonKey = "delivery:destination:lon:" + deliveryId;
        return Mono.zip(
                redisTemplate.opsForValue().get(latKey).map(Double::parseDouble),
                redisTemplate.opsForValue().get(lonKey).map(Double::parseDouble)
        ).map(tuple -> new DeliveryDestination(tuple.getT1(), tuple.getT2()));
    }
}
