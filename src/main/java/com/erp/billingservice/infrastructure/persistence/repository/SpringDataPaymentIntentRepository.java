package com.erp.billingservice.infrastructure.persistence.repository;

import com.erp.billingservice.infrastructure.persistence.entity.PaymentIntentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataPaymentIntentRepository extends JpaRepository<PaymentIntentJpaEntity, UUID> {
    Optional<PaymentIntentJpaEntity> findByExternalReference(String externalReference);
    Optional<PaymentIntentJpaEntity> findByPreferenceId(String preferenceId);
}
