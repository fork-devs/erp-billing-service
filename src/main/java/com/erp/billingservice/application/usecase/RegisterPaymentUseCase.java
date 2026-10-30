package com.erp.billingservice.application.usecase;

import com.erp.billingservice.domain.model.Currency;
import com.erp.billingservice.domain.model.Payment;

import java.math.BigDecimal;
import java.util.UUID;

public interface RegisterPaymentUseCase {

    Payment registerPayment(RegisterPaymentCommand command);

    record RegisterPaymentCommand(
            UUID invoiceId,
            String provider,
            String providerPaymentId,
            String idempotencyKey,
            BigDecimal amount,
            Currency currency
    ) {}
}
