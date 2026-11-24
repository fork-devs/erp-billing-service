package com.erp.billingservice.infrastructure.client.factos;

import com.erp.billingservice.domain.model.FiscalEmissionResult;
import com.erp.billingservice.domain.model.Invoice;
import com.erp.billingservice.domain.port.FiscalProviderPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default internal voucher adapter used when electronic invoicing (Factos/SUNAT)
 * is disabled ({@code billing.electronic-invoicing.enabled=false}).
 */
@Component
@ConditionalOnProperty(name = "billing.electronic-invoicing.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpFiscalProviderAdapter implements FiscalProviderPort {

    private static final Logger log = LoggerFactory.getLogger(NoOpFiscalProviderAdapter.class);

    @Override
    public FiscalEmissionResult emitDocument(Invoice invoice, String issuerRuc) {
        log.debug("Electronic invoicing disabled; issuing internal voucher {}-{} without Factos/SUNAT call",
                invoice.getSeries().getValue(), invoice.getCorrelative());
        return new FiscalEmissionResult("DRAFT", null, null);
    }

    @Override
    public byte[] downloadPdf(String series, String correlative) {
        return new byte[0];
    }

    @Override
    public byte[] generateQr(String content) {
        return new byte[0];
    }
}
