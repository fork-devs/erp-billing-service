package com.erp.billingservice.infrastructure.persistence.adapter;

import com.erp.billingservice.domain.model.Invoice;
import com.erp.billingservice.domain.port.InvoiceRepositoryPort;
import com.erp.billingservice.infrastructure.persistence.entity.InvoiceEntity;
import com.erp.billingservice.infrastructure.persistence.mapper.InvoiceEntityMapper;
import com.erp.billingservice.infrastructure.persistence.repository.SpringDataInvoiceRepository;
import com.erp.billingservice.infrastructure.persistence.rls.TenantContextHolder;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class InvoicePersistenceAdapter implements InvoiceRepositoryPort {

    private final SpringDataInvoiceRepository repository;

    public InvoicePersistenceAdapter(SpringDataInvoiceRepository repository) {
        this.repository = repository;
    }

    @Override
    public Invoice save(Invoice invoice) {
        InvoiceEntity entity = InvoiceEntityMapper.toEntity(invoice);
        InvoiceEntity saved = repository.save(entity);
        return InvoiceEntityMapper.toDomain(saved);
    }

    @Override
    public Optional<Invoice> findById(UUID id) {
        return repository.findById(id).map(InvoiceEntityMapper::toDomain);
    }

    @Override
    public Optional<Invoice> findByWorkOrderId(UUID workOrderId) {
        return repository.findByWorkOrderId(workOrderId).map(InvoiceEntityMapper::toDomain);
    }

    @Override
    public Optional<Invoice> findBySeriesAndCorrelative(UUID tenantId, String series, int correlative) {
        return repository.findByTenantIdAndSeriesAndCorrelativo(tenantId, series, correlative)
                .map(InvoiceEntityMapper::toDomain);
    }

    @Override
    public int getNextCorrelative(UUID tenantId, String series) {
        return repository.findMaxCorrelativoByTenantIdAndSeries(tenantId, series) + 1;
    }

    @Override
    public List<Invoice> findAll(int page, int size) {
        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            return List.of();
        }
        return repository.findAllByTenantIdOrderByIssuedAtDesc(tenantId, PageRequest.of(page, size))
                .map(InvoiceEntityMapper::toDomain)
                .getContent();
    }
}
