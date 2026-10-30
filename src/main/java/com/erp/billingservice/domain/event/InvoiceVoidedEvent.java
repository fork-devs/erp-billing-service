package com.erp.billingservice.domain.event;

import java.time.Instant;
import java.util.UUID;

public record InvoiceVoidedEvent(
        UUID eventId,
        UUID tenantId,
        UUID invoiceId,
        String series,
        int correlative,
        String reason,
        Instant occurredAt
) {
    public static InvoiceVoidedEvent of(UUID tenantId, UUID invoiceId,
                                       String series, int correlative, String reason) {
        return new InvoiceVoidedEvent(
                UUID.randomUUID(), tenantId, invoiceId, series, correlative, reason, Instant.now()
        );
    }
}
