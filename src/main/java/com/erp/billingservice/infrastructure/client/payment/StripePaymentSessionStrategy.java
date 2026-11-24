package com.erp.billingservice.infrastructure.client.payment;

import com.erp.billingservice.domain.port.PaymentSessionStrategy;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class StripePaymentSessionStrategy implements PaymentSessionStrategy {

    @Override
    public String provider() {
        return "STRIPE";
    }

    @Override
    public PaymentSessionResult createSession(UUID sessionId, UUID quoteId, UUID invoiceId) {
        return new PaymentSessionResult(
                sessionId,
                provider(),
                quoteId,
                invoiceId,
                "PENDING",
                "https://checkout.stripe.com/c/pay/cs_" + sessionId,
                Instant.now()
        );
    }
}
