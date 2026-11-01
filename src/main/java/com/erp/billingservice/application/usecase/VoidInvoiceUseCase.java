package com.erp.billingservice.application.usecase;

import com.erp.billingservice.domain.model.Invoice;

import java.util.UUID;

public interface VoidInvoiceUseCase {
    Invoice voidInvoice(UUID invoiceId, String reason);
}
