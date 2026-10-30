package com.erp.billingservice.domain.model;

import com.erp.billingservice.domain.exception.DomainValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Payment {
    private final UUID id;
    private final UUID tenantId;
    private final UUID invoiceId;
    private final String provider;
    private final String providerPaymentId;
    private final String idempotencyKey;
    private final BigDecimal amount;
    private final Currency currency;
    private PaymentStatus status;
    private final Instant createdAt;

    public Payment(UUID id, UUID tenantId, UUID invoiceId, String provider,
                   String providerPaymentId, String idempotencyKey,
                   BigDecimal amount, Currency currency, PaymentStatus status,
                   Instant createdAt) {

        if (tenantId == null) {
            throw new DomainValidationException("Tenant ID is required");
        }
        if (invoiceId == null) {
            throw new DomainValidationException("Invoice ID is required");
        }
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            throw new DomainValidationException("Idempotency key is required");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainValidationException("Payment amount must be greater than zero");
        }

        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = tenantId;
        this.invoiceId = invoiceId;
        this.provider = (provider != null && !provider.trim().isEmpty()) ? provider.trim().toUpperCase() : "CASH";
        this.providerPaymentId = providerPaymentId;
        this.idempotencyKey = idempotencyKey.trim();
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.currency = currency != null ? currency : Currency.PEN;
        this.status = status != null ? status : PaymentStatus.PENDING;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public void markConfirmed() {
        this.status = PaymentStatus.CONFIRMED;
    }

    public void markFailed() {
        this.status = PaymentStatus.FAILED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getInvoiceId() {
        return invoiceId;
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderPaymentId() {
        return providerPaymentId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Currency getCurrency() {
        return currency;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Payment payment)) return false;
        return Objects.equals(id, payment.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
