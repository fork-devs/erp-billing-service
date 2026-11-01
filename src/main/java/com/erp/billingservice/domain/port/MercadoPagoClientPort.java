package com.erp.billingservice.domain.port;

import java.math.BigDecimal;
import java.util.Optional;

public interface MercadoPagoClientPort {
    Optional<PaymentVerification> verifyPayment(String paymentId);

    record PaymentVerification(
            String paymentId,
            String status,
            BigDecimal transactionAmount,
            String paymentMethodId,
            String currencyId
    ) {}
}
