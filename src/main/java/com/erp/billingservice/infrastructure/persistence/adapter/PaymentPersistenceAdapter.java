package com.erp.billingservice.infrastructure.persistence.adapter;

import com.erp.billingservice.domain.model.Payment;
import com.erp.billingservice.domain.port.PaymentRepositoryPort;
import com.erp.billingservice.infrastructure.persistence.entity.PaymentEntity;
import com.erp.billingservice.infrastructure.persistence.mapper.PaymentEntityMapper;
import com.erp.billingservice.infrastructure.persistence.repository.SpringDataPaymentRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class PaymentPersistenceAdapter implements PaymentRepositoryPort {

    private final SpringDataPaymentRepository repository;

    public PaymentPersistenceAdapter(SpringDataPaymentRepository repository) {
        this.repository = repository;
    }

    @Override
    public Payment save(Payment payment) {
        PaymentEntity entity = PaymentEntityMapper.toEntity(payment);
        PaymentEntity saved = repository.save(entity);
        return PaymentEntityMapper.toDomain(saved);
    }

    @Override
    public Optional<Payment> findByIdempotencyKey(UUID tenantId, String idempotencyKey) {
        return repository.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey)
                .map(PaymentEntityMapper::toDomain);
    }

    @Override
    public List<Payment> findByInvoiceId(UUID invoiceId) {
        return repository.findAllByInvoiceIdOrderByCreatedAtDesc(invoiceId).stream()
                .map(PaymentEntityMapper::toDomain)
                .toList();
    }
}
