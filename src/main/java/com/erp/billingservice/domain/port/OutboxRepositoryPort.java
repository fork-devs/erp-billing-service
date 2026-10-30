package com.erp.billingservice.domain.port;

import java.util.UUID;

public interface OutboxRepositoryPort {
    void save(UUID tenantId, UUID aggregateId, UUID eventId, String eventType, String payloadJson);
}
