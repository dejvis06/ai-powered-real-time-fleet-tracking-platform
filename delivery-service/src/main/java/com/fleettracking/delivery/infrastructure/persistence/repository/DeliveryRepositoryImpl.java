package com.fleettracking.delivery.infrastructure.persistence.repository;

import com.fleettracking.delivery.domain.model.Delivery;
import com.fleettracking.delivery.domain.model.DeliveryId;
import com.fleettracking.delivery.domain.repository.DeliveryRepository;
import com.fleettracking.delivery.infrastructure.persistence.mapper.DeliveryMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class DeliveryRepositoryImpl implements DeliveryRepository {

    private final SpringDataDeliveryRepository springDataRepository;
    private final DeliveryMapper mapper;

    public DeliveryRepositoryImpl(SpringDataDeliveryRepository springDataRepository, DeliveryMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    public Delivery save(Delivery delivery) {
        var entity = mapper.toEntity(delivery);
        springDataRepository.save(entity);
        return delivery;
    }

    @Override
    public Optional<Delivery> findById(DeliveryId id) {
        return springDataRepository.findById(id.value())
                .map(mapper::toDomain);
    }
}
