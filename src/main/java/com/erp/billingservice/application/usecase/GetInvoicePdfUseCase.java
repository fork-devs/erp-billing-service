package com.erp.billingservice.application.usecase;

import java.util.UUID;

public interface GetInvoicePdfUseCase {
    byte[] getPdf(UUID invoiceId);
}
