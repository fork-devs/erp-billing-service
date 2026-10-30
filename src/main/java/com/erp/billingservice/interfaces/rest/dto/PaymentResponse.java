package com.erp.billingservice.interfaces.rest.dto;

import com.erp.billingservice.domain.model.Payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID tenantId,
        UUID invoiceId,
        String provider,
        String providerPaymentId,
        String idempotencyKey,
        BigDecimal amount,
        String currency,
        String status,
        Instant createdAt
) {
    public static PaymentResponse fromDomain(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getTenantId(),
                payment.getInvoiceId(),
                payment.getProvider(),
                payment.getProviderPaymentId(),
                payment.getIdempotencyKey(),
                payment.getAmount(),
                payment.getCurrency().name(),
                payment.getStatus().name(),
                payment.getCreatedAt()
        );
    }
}
