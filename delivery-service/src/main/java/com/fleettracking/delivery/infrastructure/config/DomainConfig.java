package com.fleettracking.delivery.infrastructure.config;

import com.fleettracking.delivery.domain.service.DefaultDeliveryDomainService;
import com.fleettracking.delivery.domain.service.DeliveryDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainConfig {

    @Bean
    public DeliveryDomainService deliveryDomainService() {
        return new DefaultDeliveryDomainService();
    }
}
