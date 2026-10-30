package com.erp.billingservice.infrastructure.persistence.repository;

import com.erp.billingservice.infrastructure.persistence.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataPaymentRepository extends JpaRepository<PaymentEntity, UUID> {

    Optional<PaymentEntity> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);

    List<PaymentEntity> findAllByInvoiceIdOrderByCreatedAtDesc(UUID invoiceId);
}
