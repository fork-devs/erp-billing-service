package com.erp.billingservice.domain;

import com.erp.billingservice.domain.event.InvoiceVoidedEvent;
import com.erp.billingservice.domain.exception.DomainValidationException;
import com.erp.billingservice.domain.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DomainValueObjectsAndModelsTest {

    @Test
    @DisplayName("InvoiceVoidedEvent creation")
    void testInvoiceVoidedEvent() {
        UUID tenantId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        InvoiceVoidedEvent event = InvoiceVoidedEvent.of(tenantId, invoiceId, "F001", 1, "Wrong line item");
        assertNotNull(event.eventId());
        assertEquals(tenantId, event.tenantId());
        assertEquals(invoiceId, event.invoiceId());
        assertEquals("F001", event.series());
        assertEquals(1, event.correlative());
        assertEquals("Wrong line item", event.reason());
        assertNotNull(event.occurredAt());
    }

    @Test
    @DisplayName("Dni value object validation and methods")
    void testDni() {
        Dni dni1 = new Dni("12345678");
        Dni dni2 = new Dni("12345678");
        Dni dni3 = new Dni("87654321");

        assertEquals("12345678", dni1.getValue());
        assertEquals("12345678", dni1.toString());
        assertEquals(dni1, dni2);
        assertNotEquals(dni1, dni3);
        assertNotEquals(dni1, null);
        assertNotEquals(dni1, "12345678");
        assertEquals(dni1.hashCode(), dni2.hashCode());

        assertThrows(DomainValidationException.class, () -> new Dni(null));
        assertThrows(DomainValidationException.class, () -> new Dni(""));
        assertThrows(DomainValidationException.class, () -> new Dni("1234567"));
        assertThrows(DomainValidationException.class, () -> new Dni("123456789"));
        assertThrows(DomainValidationException.class, () -> new Dni("1234567a"));
    }

    @Test
    @DisplayName("Ruc value object validation and methods")
    void testRuc() {
        Ruc ruc1 = new Ruc("20123456789");
        Ruc ruc2 = new Ruc("20123456789");
        Ruc ruc3 = new Ruc("10123456789");

        assertEquals("20123456789", ruc1.getValue());
        assertEquals("20123456789", ruc1.toString());
        assertEquals(ruc1, ruc2);
        assertNotEquals(ruc1, ruc3);
        assertNotEquals(ruc1, null);
        assertNotEquals(ruc1, "20123456789");
        assertEquals(ruc1.hashCode(), ruc2.hashCode());

        // Valid prefixes: 10, 20, 15, 16, 17
        assertDoesNotThrow(() -> new Ruc("15123456789"));
        assertDoesNotThrow(() -> new Ruc("16123456789"));
        assertDoesNotThrow(() -> new Ruc("17123456789"));

        assertThrows(DomainValidationException.class, () -> new Ruc(null));
        assertThrows(DomainValidationException.class, () -> new Ruc(""));
        assertThrows(DomainValidationException.class, () -> new Ruc("30123456789")); // invalid prefix
        assertThrows(DomainValidationException.class, () -> new Ruc("2012345678")); // 10 digits
        assertThrows(DomainValidationException.class, () -> new Ruc("201234567890")); // 12 digits
    }

    @Test
    @DisplayName("Series value object validation and methods")
    void testSeries() {
        Series s1 = new Series("F001");
        Series s2 = new Series("F001");
        Series s3 = new Series("B001");

        assertEquals("F001", s1.getValue());
        assertEquals("F001", s1.toString());
        assertEquals(s1, s2);
        assertNotEquals(s1, s3);
        assertNotEquals(s1, null);
        assertNotEquals(s1, "F001");
        assertEquals(s1.hashCode(), s2.hashCode());

        assertDoesNotThrow(() -> s1.validateFor(VoucherType.FACTURA));
        assertThrows(DomainValidationException.class, () -> s1.validateFor(VoucherType.BOLETA));

        assertDoesNotThrow(() -> s3.validateFor(VoucherType.BOLETA));
        assertThrows(DomainValidationException.class, () -> s3.validateFor(VoucherType.FACTURA));

        assertThrows(DomainValidationException.class, () -> new Series(null));
        assertThrows(DomainValidationException.class, () -> new Series(""));
        assertThrows(DomainValidationException.class, () -> new Series("F01")); // too short
        assertThrows(DomainValidationException.class, () -> new Series("F0001")); // too long
        assertThrows(DomainValidationException.class, () -> new Series("A001")); // invalid prefix
    }

    @Test
    @DisplayName("Correlative value object validation and methods")
    void testCorrelative() {
        Correlative c1 = new Correlative(42);
        Correlative c2 = new Correlative(42);
        Correlative c3 = new Correlative(100);

        assertEquals(42, c1.getValue());
        assertEquals("00000042", c1.toFormattedString());
        assertEquals("00000042", c1.toString());
        assertEquals(c1, c2);
        assertNotEquals(c1, c3);
        assertNotEquals(c1, null);
        assertNotEquals(c1, 42);
        assertEquals(c1.hashCode(), c2.hashCode());

        assertThrows(DomainValidationException.class, () -> new Correlative(0));
        assertThrows(DomainValidationException.class, () -> new Correlative(-5));
        assertThrows(DomainValidationException.class, () -> new Correlative(100_000_000));
    }

    @Test
    @DisplayName("VoucherType enum methods")
    void testVoucherType() {
        assertEquals("01", VoucherType.FACTURA.getCpeCode());
        assertEquals("F", VoucherType.FACTURA.getSeriesPrefix());
        assertEquals("03", VoucherType.BOLETA.getCpeCode());
        assertEquals("B", VoucherType.BOLETA.getSeriesPrefix());

        assertEquals(VoucherType.FACTURA, VoucherType.fromCpeCode("01"));
        assertEquals(VoucherType.BOLETA, VoucherType.fromCpeCode("03"));
        assertThrows(IllegalArgumentException.class, () -> VoucherType.fromCpeCode("99"));
    }

    @Test
    @DisplayName("InvoiceLine invariants and methods")
    void testInvoiceLine() {
        UUID lineId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        InvoiceLine line = new InvoiceLine(lineId, tenantId, "CODE-01", "Oil Filter",
                new BigDecimal("2"), new BigDecimal("25.50"), "TAXABLE_ONEROUS");

        assertEquals(lineId, line.getId());
        assertEquals(tenantId, line.getTenantId());
        assertEquals("CODE-01", line.getCode());
        assertEquals("Oil Filter", line.getDescription());
        assertEquals(new BigDecimal("2.00"), line.getQuantity());
        assertEquals(new BigDecimal("25.50"), line.getUnitPrice());
        assertEquals(new BigDecimal("51.00"), line.getSubtotal());
        assertEquals("TAXABLE_ONEROUS", line.getAffectationType());

        InvoiceLine lineSameId = new InvoiceLine(lineId, tenantId, "OTHER", "Other",
                BigDecimal.ONE, BigDecimal.TEN, null);
        assertEquals("TAXABLE_ONEROUS", lineSameId.getAffectationType()); // default affectation
        assertEquals(line, lineSameId);
        assertEquals(line.hashCode(), lineSameId.hashCode());
        assertNotEquals(line, null);
        assertNotEquals(line, "string");

        InvoiceLine lineDefaultId = new InvoiceLine(null, tenantId, "C", "D",
                BigDecimal.ONE, BigDecimal.ZERO, "   ");
        assertNotNull(lineDefaultId.getId());
        assertEquals("TAXABLE_ONEROUS", lineDefaultId.getAffectationType());

        // Validation failures
        assertThrows(DomainValidationException.class, () -> new InvoiceLine(null, tenantId, null, "D", BigDecimal.ONE, BigDecimal.ONE, ""));
        assertThrows(DomainValidationException.class, () -> new InvoiceLine(null, tenantId, " ", "D", BigDecimal.ONE, BigDecimal.ONE, ""));
        assertThrows(DomainValidationException.class, () -> new InvoiceLine(null, tenantId, "C", null, BigDecimal.ONE, BigDecimal.ONE, ""));
        assertThrows(DomainValidationException.class, () -> new InvoiceLine(null, tenantId, "C", "  ", BigDecimal.ONE, BigDecimal.ONE, ""));
        assertThrows(DomainValidationException.class, () -> new InvoiceLine(null, tenantId, "C", "D", null, BigDecimal.ONE, ""));
        assertThrows(DomainValidationException.class, () -> new InvoiceLine(null, tenantId, "C", "D", BigDecimal.ZERO, BigDecimal.ONE, ""));
        assertThrows(DomainValidationException.class, () -> new InvoiceLine(null, tenantId, "C", "D", new BigDecimal("-1"), BigDecimal.ONE, ""));
        assertThrows(DomainValidationException.class, () -> new InvoiceLine(null, tenantId, "C", "D", BigDecimal.ONE, null, ""));
        assertThrows(DomainValidationException.class, () -> new InvoiceLine(null, tenantId, "C", "D", BigDecimal.ONE, new BigDecimal("-0.01"), ""));
    }

    @Test
    @DisplayName("Payment domain model invariants and state transitions")
    void testPayment() {
        UUID paymentId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        Instant createdAt = Instant.now();

        Payment payment = new Payment(paymentId, tenantId, invoiceId, "MERCADO_PAGO",
                "mp-pay-123", "idemp-001", new BigDecimal("150.00"), Currency.PEN,
                PaymentStatus.PENDING, createdAt);

        assertEquals(paymentId, payment.getId());
        assertEquals(tenantId, payment.getTenantId());
        assertEquals(invoiceId, payment.getInvoiceId());
        assertEquals("MERCADO_PAGO", payment.getProvider());
        assertEquals("mp-pay-123", payment.getProviderPaymentId());
        assertEquals("idemp-001", payment.getIdempotencyKey());
        assertEquals(new BigDecimal("150.00"), payment.getAmount());
        assertEquals(Currency.PEN, payment.getCurrency());
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        assertEquals(createdAt, payment.getCreatedAt());

        // State changes
        payment.markConfirmed();
        assertEquals(PaymentStatus.CONFIRMED, payment.getStatus());

        payment.markFailed();
        assertEquals(PaymentStatus.FAILED, payment.getStatus());

        // Equals and hashCode
        Payment same = new Payment(paymentId, tenantId, invoiceId, null, null, "idemp-002",
                new BigDecimal("50.00"), null, null, null);
        assertEquals(payment, same);
        assertEquals(payment.hashCode(), same.hashCode());
        assertNotEquals(payment, null);
        assertNotEquals(payment, "str");

        // Defaults
        assertEquals("CASH", same.getProvider());
        assertEquals(Currency.PEN, same.getCurrency());
        assertEquals(PaymentStatus.PENDING, same.getStatus());
        assertNotNull(same.getCreatedAt());

        // Validation exceptions
        assertThrows(DomainValidationException.class, () -> new Payment(null, null, invoiceId, "CASH", null, "k", BigDecimal.TEN, Currency.PEN, null, null));
        assertThrows(DomainValidationException.class, () -> new Payment(null, tenantId, null, "CASH", null, "k", BigDecimal.TEN, Currency.PEN, null, null));
        assertThrows(DomainValidationException.class, () -> new Payment(null, tenantId, invoiceId, "CASH", null, null, BigDecimal.TEN, Currency.PEN, null, null));
        assertThrows(DomainValidationException.class, () -> new Payment(null, tenantId, invoiceId, "CASH", null, "  ", BigDecimal.TEN, Currency.PEN, null, null));
        assertThrows(DomainValidationException.class, () -> new Payment(null, tenantId, invoiceId, "CASH", null, "k", null, Currency.PEN, null, null));
        assertThrows(DomainValidationException.class, () -> new Payment(null, tenantId, invoiceId, "CASH", null, "k", BigDecimal.ZERO, Currency.PEN, null, null));
        assertThrows(DomainValidationException.class, () -> new Payment(null, tenantId, invoiceId, "CASH", null, "k", new BigDecimal("-10"), Currency.PEN, null, null));
    }

    @Test
    @DisplayName("Invoice domain model full lifecycle, getters, and validations")
    void testInvoiceFull() {
        UUID invoiceId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID workOrderId = UUID.randomUUID();
        UUID quoteId = UUID.randomUUID();

        InvoiceLine line = new InvoiceLine(
                UUID.randomUUID(), tenantId, "SERV-01", "Diagnostics",
                BigDecimal.ONE, new BigDecimal("100.00"), "TAXABLE_ONEROUS"
        );

        Invoice invoice = new Invoice(
                invoiceId, tenantId, workOrderId, quoteId, VoucherType.BOLETA,
                new Series("B001"), new Correlative(10), "72345678", "JOHN DOE",
                Currency.PEN, List.of(line), InvoiceStatus.DRAFT, null, null, null,
                Instant.now(), null
        );

        assertEquals(invoiceId, invoice.getId());
        assertEquals(tenantId, invoice.getTenantId());
        assertEquals(workOrderId, invoice.getWorkOrderId());
        assertEquals(quoteId, invoice.getQuoteId());
        assertEquals(VoucherType.BOLETA, invoice.getVoucherType());
        assertEquals(new Series("B001"), invoice.getSeries());
        assertEquals(new Correlative(10), invoice.getCorrelative());
        assertEquals("72345678", invoice.getCustomerDocument());
        assertEquals("JOHN DOE", invoice.getCustomerName());
        assertEquals(Currency.PEN, invoice.getCurrency());
        assertEquals(1, invoice.getLines().size());
        assertNotNull(invoice.getIssuedAt());
        assertNull(invoice.getVoidedAt());
        assertNull(invoice.getVoidReason());
        assertNull(invoice.getPdfUrl());
        assertNull(invoice.getQrContent());

        // Observed state
        invoice.markObserved();
        assertEquals(InvoiceStatus.OBSERVED, invoice.getSunatStatus());

        // Equals & hash
        Invoice same = new Invoice(
                invoiceId, tenantId, workOrderId, VoucherType.BOLETA,
                new Series("B001"), new Correlative(11), "72345678", "JOHN",
                null, List.of(line), null, null, null, null, null, null
        );
        assertEquals(invoice, same);
        assertEquals(invoice.hashCode(), same.hashCode());
        assertNotEquals(invoice, null);
        assertNotEquals(invoice, "other");

        // Validation exceptions
        assertThrows(DomainValidationException.class, () -> new Invoice(
                null, null, workOrderId, VoucherType.BOLETA,
                new Series("B001"), new Correlative(1), "72345678", "JOHN",
                Currency.PEN, List.of(line), null, null, null, null, null, null
        ));
        assertThrows(DomainValidationException.class, () -> new Invoice(
                null, tenantId, workOrderId, null,
                new Series("B001"), new Correlative(1), "72345678", "JOHN",
                Currency.PEN, List.of(line), null, null, null, null, null, null
        ));
        assertThrows(DomainValidationException.class, () -> new Invoice(
                null, tenantId, workOrderId, VoucherType.BOLETA,
                null, new Correlative(1), "72345678", "JOHN",
                Currency.PEN, List.of(line), null, null, null, null, null, null
        ));
        assertThrows(DomainValidationException.class, () -> new Invoice(
                null, tenantId, workOrderId, VoucherType.BOLETA,
                new Series("B001"), null, "72345678", "JOHN",
                Currency.PEN, List.of(line), null, null, null, null, null, null
        ));
        assertThrows(DomainValidationException.class, () -> new Invoice(
                null, tenantId, workOrderId, VoucherType.BOLETA,
                new Series("B001"), new Correlative(1), null, "JOHN",
                Currency.PEN, List.of(line), null, null, null, null, null, null
        ));
        assertThrows(DomainValidationException.class, () -> new Invoice(
                null, tenantId, workOrderId, VoucherType.BOLETA,
                new Series("B001"), new Correlative(1), "   ", "JOHN",
                Currency.PEN, List.of(line), null, null, null, null, null, null
        ));
        assertThrows(DomainValidationException.class, () -> new Invoice(
                null, tenantId, workOrderId, VoucherType.BOLETA,
                new Series("B001"), new Correlative(1), "72345678", null,
                Currency.PEN, List.of(line), null, null, null, null, null, null
        ));
        assertThrows(DomainValidationException.class, () -> new Invoice(
                null, tenantId, workOrderId, VoucherType.BOLETA,
                new Series("B001"), new Correlative(1), "72345678", "  ",
                Currency.PEN, List.of(line), null, null, null, null, null, null
        ));
        assertThrows(DomainValidationException.class, () -> new Invoice(
                null, tenantId, workOrderId, VoucherType.BOLETA,
                new Series("B001"), new Correlative(1), "72345678", "JOHN",
                Currency.PEN, null, null, null, null, null, null, null
        ));
        assertThrows(DomainValidationException.class, () -> new Invoice(
                null, tenantId, workOrderId, VoucherType.BOLETA,
                new Series("B001"), new Correlative(1), "72345678", "JOHN",
                Currency.PEN, Collections.emptyList(), null, null, null, null, null, null
        ));

        // Void validation
        assertThrows(DomainValidationException.class, () -> invoice.voidInvoice(null));
        assertThrows(DomainValidationException.class, () -> invoice.voidInvoice("  "));

        invoice.voidInvoice("Customer requested cancellation");
        assertThrows(DomainValidationException.class, () -> invoice.voidInvoice("Again"));
        assertThrows(DomainValidationException.class, invoice::markObserved);
    }
}
