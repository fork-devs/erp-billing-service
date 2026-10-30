package com.erp.billingservice.domain;

import com.erp.billingservice.domain.exception.DomainValidationException;
import com.erp.billingservice.domain.model.Correlative;
import com.erp.billingservice.domain.model.Currency;
import com.erp.billingservice.domain.model.Invoice;
import com.erp.billingservice.domain.model.InvoiceLine;
import com.erp.billingservice.domain.model.InvoiceStatus;
import com.erp.billingservice.domain.model.Series;
import com.erp.billingservice.domain.model.VoucherType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InvoiceCalculationTest {

    private final UUID tenantId = UUID.randomUUID();
    private final UUID workOrderId = UUID.randomUUID();

    @Test
    @DisplayName("Should correctly calculate taxable base and 18% IGV for an Invoice")
    void shouldCalculateTaxableAndIgvForFactura() {
        InvoiceLine line = new InvoiceLine(
                UUID.randomUUID(), tenantId, "SERV-004", "Production Deployment",
                new BigDecimal("1"), new BigDecimal("5900.00"), "TAXABLE_ONEROUS"
        );

        Invoice invoice = new Invoice(
                UUID.randomUUID(), tenantId, workOrderId, VoucherType.FACTURA,
                new Series("F004"), new Correlative(1), "20111222333",
                "ENTERPRISE S.A.C.", Currency.PEN, List.of(line),
                InvoiceStatus.DRAFT, null, null, null, null, null
        );

        assertEquals(new BigDecimal("5900.00"), invoice.getTotalAmount());
        assertEquals(new BigDecimal("5000.00"), invoice.getTotalTaxable());
        assertEquals(new BigDecimal("900.00"), invoice.getTotalIgv());
    }

    @Test
    @DisplayName("Should correctly calculate taxable base and IGV with half-up rounding for a Boleta")
    void shouldCalculateTaxableAndIgvForBoleta() {
        InvoiceLine line = new InvoiceLine(
                UUID.randomUUID(), tenantId, "PROD-B01", "Workshop Product",
                new BigDecimal("2"), new BigDecimal("50.00"), "TAXABLE_ONEROUS"
        );

        Invoice invoice = new Invoice(
                UUID.randomUUID(), tenantId, workOrderId, VoucherType.BOLETA,
                new Series("B001"), new Correlative(1), "72345678",
                "JOHN DOE", Currency.PEN, List.of(line),
                InvoiceStatus.DRAFT, null, null, null, null, null
        );

        assertEquals(new BigDecimal("100.00"), invoice.getTotalAmount());
        assertEquals(new BigDecimal("84.75"), invoice.getTotalTaxable());
        assertEquals(new BigDecimal("15.25"), invoice.getTotalIgv());
    }

    @Test
    @DisplayName("Should reject Factura if customer document is not an 11-digit RUC")
    void shouldRejectFacturaWithNonRucDocument() {
        InvoiceLine line = new InvoiceLine(
                UUID.randomUUID(), tenantId, "SERV-01", "Service",
                new BigDecimal("1"), new BigDecimal("100.00"), "TAXABLE_ONEROUS"
        );

        assertThrows(DomainValidationException.class, () -> new Invoice(
                UUID.randomUUID(), tenantId, workOrderId, VoucherType.FACTURA,
                new Series("F001"), new Correlative(1), "72345678", // 8-digit DNI
                "ENTERPRISE", Currency.PEN, List.of(line),
                InvoiceStatus.DRAFT, null, null, null, null, null
        ));
    }

    @Test
    @DisplayName("Should reject Factura if series does not start with F")
    void shouldRejectFacturaWithInvalidSeriesPrefix() {
        InvoiceLine line = new InvoiceLine(
                UUID.randomUUID(), tenantId, "SERV-01", "Service",
                new BigDecimal("1"), new BigDecimal("100.00"), "TAXABLE_ONEROUS"
        );

        assertThrows(DomainValidationException.class, () -> new Invoice(
                UUID.randomUUID(), tenantId, workOrderId, VoucherType.FACTURA,
                new Series("B001"), new Correlative(1), "20111222333",
                "ENTERPRISE", Currency.PEN, List.of(line),
                InvoiceStatus.DRAFT, null, null, null, null, null
        ));
    }

    @Test
    @DisplayName("Should enforce valid lifecycle state transitions and prevent mutating voided invoices")
    void shouldHandleValidStateTransitions() {
        InvoiceLine line = new InvoiceLine(
                UUID.randomUUID(), tenantId, "SERV-01", "Service",
                new BigDecimal("1"), new BigDecimal("100.00"), "TAXABLE_ONEROUS"
        );

        Invoice invoice = new Invoice(
                UUID.randomUUID(), tenantId, workOrderId, VoucherType.BOLETA,
                new Series("B001"), new Correlative(1), "72345678",
                "CUSTOMER", Currency.PEN, List.of(line),
                InvoiceStatus.DRAFT, null, null, null, null, null
        );

        assertEquals(InvoiceStatus.DRAFT, invoice.getSunatStatus());

        invoice.markPendingSunat();
        assertEquals(InvoiceStatus.PENDING_SUNAT, invoice.getSunatStatus());

        invoice.markAccepted("/api/v1/rendering/pdf/B001/00000001", "QR_DATA");
        assertEquals(InvoiceStatus.ACCEPTED, invoice.getSunatStatus());
        assertEquals("/api/v1/rendering/pdf/B001/00000001", invoice.getPdfUrl());

        invoice.voidInvoice("Input error in line items");
        assertEquals(InvoiceStatus.VOIDED, invoice.getSunatStatus());
        assertNotNull(invoice.getVoidedAt());
        assertEquals("Input error in line items", invoice.getVoidReason());

        // Once voided, cannot be accepted
        assertThrows(DomainValidationException.class, () -> invoice.markAccepted("url", "qr"));
    }
}
