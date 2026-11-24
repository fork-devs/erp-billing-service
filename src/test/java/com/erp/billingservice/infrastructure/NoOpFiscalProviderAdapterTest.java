package com.erp.billingservice.infrastructure;

import com.erp.billingservice.domain.model.Correlative;
import com.erp.billingservice.domain.model.Currency;
import com.erp.billingservice.domain.model.FiscalEmissionResult;
import com.erp.billingservice.domain.model.Invoice;
import com.erp.billingservice.domain.model.InvoiceLine;
import com.erp.billingservice.domain.model.InvoiceStatus;
import com.erp.billingservice.domain.model.Series;
import com.erp.billingservice.domain.model.VoucherType;
import com.erp.billingservice.infrastructure.client.factos.NoOpFiscalProviderAdapter;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NoOpFiscalProviderAdapterTest {

    @Test
    void emitsInternalVoucherWithoutCallingFactosWhenDisabled() {
        NoOpFiscalProviderAdapter adapter = new NoOpFiscalProviderAdapter();
        UUID tenantId = UUID.randomUUID();
        InvoiceLine line = new InvoiceLine(
                UUID.randomUUID(), tenantId, "SERV-01", "Maintenance",
                BigDecimal.ONE, new BigDecimal("100.00"), "TAXABLE_ONEROUS"
        );
        Invoice invoice = new Invoice(
                UUID.randomUUID(), tenantId, UUID.randomUUID(), VoucherType.RECEIPT,
                new Series("B001"), new Correlative(1), "48765432",
                "Maria Fernandez", Currency.PEN, List.of(line),
                InvoiceStatus.DRAFT, null, null, null, null, null
        );

        FiscalEmissionResult result = adapter.emitDocument(invoice, "20512345678");
        assertEquals("DRAFT", result.status());
        assertNull(result.pdfUrl());
        assertNull(result.qrContent());
        assertEquals(0, adapter.downloadPdf("B001", "1").length);
    }
}
