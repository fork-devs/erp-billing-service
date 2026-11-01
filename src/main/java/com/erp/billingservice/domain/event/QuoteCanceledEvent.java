package com.erp.billingservice.domain.event;

import java.time.Instant;
import java.util.UUID;

public record QuoteCanceledEvent(
        UUID eventId,
        UUID tenantId,
        UUID quoteId,
        UUID workOrderId,
        UUID branchId,
        Instant occurredAt
) {
    public static QuoteCanceledEvent of(UUID tenantId, UUID quoteId, UUID workOrderId, UUID branchId) {
        return new QuoteCanceledEvent(
                UUID.randomUUID(), tenantId, quoteId, workOrderId, branchId, Instant.now()
        );
    }
}
