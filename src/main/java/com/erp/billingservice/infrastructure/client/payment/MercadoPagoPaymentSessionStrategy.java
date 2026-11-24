package com.erp.billingservice.infrastructure.client.payment;

import com.erp.billingservice.domain.port.PaymentSessionStrategy;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class MercadoPagoPaymentSessionStrategy implements PaymentSessionStrategy {

    @Override
    public String provider() {
        return "MERCADOPAGO";
    }

    @Override
    public PaymentSessionResult createSession(UUID sessionId, UUID quoteId, UUID invoiceId) {
        return new PaymentSessionResult(
                sessionId,
                provider(),
                quoteId,
                invoiceId,
                "PENDING",
                "https://www.mercadopago.com.pe/checkout/v1/redirect?pref_id=pref-" + sessionId,
                Instant.now()
        );
    }
}
