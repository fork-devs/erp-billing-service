package com.erp.billingservice.infrastructure.persistence.mapper;

import com.erp.billingservice.domain.model.Currency;
import com.erp.billingservice.domain.model.Payment;
import com.erp.billingservice.domain.model.PaymentStatus;
import com.erp.billingservice.infrastructure.persistence.entity.PaymentEntity;

public final class PaymentEntityMapper {

    private PaymentEntityMapper() {
    }

    public static Payment toDomain(PaymentEntity entity) {
        if (entity == null) {
            return null;
        }

        return new Payment(
                entity.getId(),
                entity.getTenantId(),
                entity.getInvoiceId(),
                entity.getProvider(),
                entity.getProviderPaymentId(),
                entity.getIdempotencyKey(),
                entity.getAmount(),
                Currency.valueOf(entity.getCurrency()),
                PaymentStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt()
        );
    }

    public static PaymentEntity toEntity(Payment domain) {
        if (domain == null) {
            return null;
        }

        return new PaymentEntity(
                domain.getId(),
                domain.getTenantId(),
                domain.getInvoiceId(),
                domain.getProvider(),
                domain.getProviderPaymentId(),
                domain.getIdempotencyKey(),
                domain.getAmount(),
                domain.getCurrency().name(),
                domain.getStatus().name(),
                domain.getCreatedAt()
        );
    }
}
