package com.erp.billingservice.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentReceivedEvent(
        UUID eventId,
        UUID tenantId,
        UUID paymentId,
        UUID invoiceId,
        String provider,
        String idempotencyKey,
        BigDecimal amount,
        String currency,
        Instant occurredAt
) {
    public static PaymentReceivedEvent of(UUID tenantId, UUID paymentId, UUID invoiceId,
                                         String provider, String idempotencyKey,
                                         BigDecimal amount, String currency) {
        return new PaymentReceivedEvent(
                UUID.randomUUID(), tenantId, paymentId, invoiceId, provider,
                idempotencyKey, amount, currency, Instant.now()
        );
    }
}
