package com.erp.billingservice.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record QuoteApprovedEvent(
        UUID eventId,
        UUID tenantId,
        UUID quoteId,
        UUID workOrderId,
        UUID branchId,
        BigDecimal totalAmount,
        Instant occurredAt
) {
    public static QuoteApprovedEvent of(UUID tenantId, UUID quoteId, UUID workOrderId, UUID branchId, BigDecimal totalAmount) {
        return new QuoteApprovedEvent(
                UUID.randomUUID(), tenantId, quoteId, workOrderId, branchId, totalAmount, Instant.now()
        );
    }
}
