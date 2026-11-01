package com.erp.billingservice.application.usecase;

import com.erp.billingservice.domain.model.Invoice;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GetInvoiceUseCase {
    Optional<Invoice> getById(UUID id);
    Optional<Invoice> getByWorkOrderId(UUID workOrderId);
    List<Invoice> listInvoices(int page, int size);
}
