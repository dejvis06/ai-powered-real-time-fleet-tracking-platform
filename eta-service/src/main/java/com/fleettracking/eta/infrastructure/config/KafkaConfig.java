package com.fleettracking.eta.infrastructure.config;

import com.fleettracking.common.events.DeliveryStartedEvent;
import com.fleettracking.common.events.VehicleLocationEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:eta-service}")
    private String groupId;

    private Map<String, Object> baseConsumerConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("bootstrap.servers", bootstrapServers);
        config.put("group.id", groupId);
        config.put("auto.offset.reset", "earliest");
        config.put("enable.auto.commit", "false");
        config.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        config.put("value.deserializer", "org.springframework.kafka.support.serializer.JsonDeserializer");
        config.put("spring.json.trusted.packages", "com.fleettracking.*");
        config.put("spring.json.use.type.headers", "false");
        return config;
    }

    @Bean
    public ConsumerFactory<String, VehicleLocationEvent> consumerFactory() {
        Map<String, Object> config = baseConsumerConfig();
        config.put("spring.json.value.default.type", "com.fleettracking.common.events.VehicleLocationEvent");
        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, VehicleLocationEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, VehicleLocationEvent> consumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, VehicleLocationEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, DeliveryStartedEvent> deliveryEventConsumerFactory() {
        Map<String, Object> config = baseConsumerConfig();
        config.put("spring.json.value.default.type", "com.fleettracking.common.events.DeliveryStartedEvent");
        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DeliveryStartedEvent> deliveryEventListenerContainerFactory(
            ConsumerFactory<String, DeliveryStartedEvent> deliveryEventConsumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, DeliveryStartedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(deliveryEventConsumerFactory);
        return factory;
    }
}
