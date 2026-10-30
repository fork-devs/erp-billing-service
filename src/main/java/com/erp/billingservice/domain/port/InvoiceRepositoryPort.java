package com.erp.billingservice.domain.port;

import com.erp.billingservice.domain.model.Invoice;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepositoryPort {
    Invoice save(Invoice invoice);
    Optional<Invoice> findById(UUID id);
    Optional<Invoice> findByWorkOrderId(UUID workOrderId);
    Optional<Invoice> findBySeriesAndCorrelative(UUID tenantId, String series, int correlative);
    int getNextCorrelative(UUID tenantId, String series);
    List<Invoice> findAll(int page, int size);
}
