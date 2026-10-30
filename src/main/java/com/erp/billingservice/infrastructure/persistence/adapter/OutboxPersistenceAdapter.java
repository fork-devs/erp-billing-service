package com.erp.billingservice.infrastructure.persistence.adapter;

import com.erp.billingservice.domain.port.OutboxRepositoryPort;
import com.erp.billingservice.infrastructure.persistence.entity.BillingOutboxEntity;
import com.erp.billingservice.infrastructure.persistence.repository.SpringDataOutboxRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class OutboxPersistenceAdapter implements OutboxRepositoryPort {

    private final SpringDataOutboxRepository repository;

    public OutboxPersistenceAdapter(SpringDataOutboxRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(UUID tenantId, UUID aggregateId, UUID eventId, String eventType, String payloadJson) {
        BillingOutboxEntity entity = new BillingOutboxEntity(
                UUID.randomUUID(),
                tenantId,
                aggregateId,
                eventId,
                eventType,
                payloadJson,
                "PENDING",
                0,
                Instant.now(),
                Instant.now()
        );
        repository.save(entity);
    }
}
