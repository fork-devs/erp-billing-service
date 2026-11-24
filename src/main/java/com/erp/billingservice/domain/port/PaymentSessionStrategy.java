package com.erp.billingservice.domain.port;

import java.time.Instant;
import java.util.UUID;

/**
 * Strategy contract for creating provider-specific payment sessions.
 */
public interface PaymentSessionStrategy {

    record PaymentSessionResult(
            UUID sessionId,
            String provider,
            UUID quoteId,
            UUID invoiceId,
            String status,
            String checkoutUrl,
            Instant createdAt
    ) {}

    String provider();

    PaymentSessionResult createSession(UUID sessionId, UUID quoteId, UUID invoiceId);
}
