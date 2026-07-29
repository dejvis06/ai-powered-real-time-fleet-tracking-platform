package com.fleettracking.proxy.infrastructure.redis;

import com.fleettracking.common.model.DeliveryStatus;
import com.fleettracking.proxy.application.port.DeliveryStateReader;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class RedisDeliveryStateReader implements DeliveryStateReader {

    private static final Logger log = LoggerFactory.getLogger(RedisDeliveryStateReader.class);

    private final ReactiveStringRedisTemplate redisTemplate;
    private final MeterRegistry meterRegistry;

    public RedisDeliveryStateReader(
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
                .doOnEmpty(() -> {
                    log.warn("No delivery status found in Redis for delivery={}", deliveryId);
                    meterRegistry.counter("redis.routing.miss").increment();
                });
    }
}
