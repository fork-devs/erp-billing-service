package com.erp.billingservice.domain.port;

import com.erp.billingservice.domain.model.Payment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepositoryPort {
    Payment save(Payment payment);
    Optional<Payment> findById(UUID id);
    Optional<Payment> findByIdempotencyKey(UUID tenantId, String idempotencyKey);
    List<Payment> findByInvoiceId(UUID invoiceId);
    void deleteById(UUID id);
}
