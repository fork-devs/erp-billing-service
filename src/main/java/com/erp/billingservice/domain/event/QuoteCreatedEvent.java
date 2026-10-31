package com.erp.billingservice.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record QuoteCreatedEvent(
        UUID eventId,
        UUID tenantId,
        UUID quoteId,
        UUID workOrderId,
        UUID branchId,
        BigDecimal subtotalAmount,
        BigDecimal discountPercentage,
        BigDecimal totalAmount,
        Instant occurredAt
) {
    public static QuoteCreatedEvent of(UUID tenantId, UUID quoteId, UUID workOrderId, UUID branchId,
                                       BigDecimal subtotalAmount, BigDecimal discountPercentage, BigDecimal totalAmount) {
        return new QuoteCreatedEvent(
                UUID.randomUUID(), tenantId, quoteId, workOrderId, branchId,
                subtotalAmount, discountPercentage, totalAmount, Instant.now()
        );
    }
}
