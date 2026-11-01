package com.erp.billingservice.domain.port;

import com.erp.billingservice.domain.model.FiscalEmissionResult;
import com.erp.billingservice.domain.model.Invoice;

public interface FiscalProviderPort {
    FiscalEmissionResult emitDocument(Invoice invoice, String issuerRuc);
    byte[] downloadPdf(String series, String correlative);
    byte[] generateQr(String content);
}
