package com.erp.billingservice.domain.model;

import com.erp.billingservice.domain.exception.DomainValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Invoice {
    private static final BigDecimal IGV_DIVISOR = new BigDecimal("1.18");

    private final UUID id;
    private final UUID tenantId;
    private final UUID workOrderId;
    private final VoucherType voucherType;
    private final Series series;
    private final Correlative correlative;
    private final String customerDocument;
    private final String customerName;
    private final Currency currency;
    private final List<InvoiceLine> lines;

    private BigDecimal totalTaxable;
    private BigDecimal totalIgv;
    private BigDecimal totalAmount;

    private InvoiceStatus sunatStatus;
    private String pdfUrl;
    private String qrContent;
    private String voidReason;
    private final Instant issuedAt;
    private Instant voidedAt;

    public Invoice(UUID id, UUID tenantId, UUID workOrderId, VoucherType voucherType,
                   Series series, Correlative correlative, String customerDocument,
                   String customerName, Currency currency, List<InvoiceLine> lines,
                   InvoiceStatus sunatStatus, String pdfUrl, String qrContent,
                   String voidReason, Instant issuedAt, Instant voidedAt) {

        if (tenantId == null) {
            throw new DomainValidationException("Tenant ID is required");
        }
        if (workOrderId == null) {
            throw new DomainValidationException("Work order ID is required");
        }
        if (voucherType == null) {
            throw new DomainValidationException("Voucher type is required");
        }
        if (series == null) {
            throw new DomainValidationException("Series is required");
        }
        series.validateFor(voucherType);

        if (correlative == null) {
            throw new DomainValidationException("Correlative is required");
        }
        if (customerDocument == null || customerDocument.trim().isEmpty()) {
            throw new DomainValidationException("Customer document is required");
        }
        validateCustomerDocument(voucherType, customerDocument.trim());

        if (customerName == null || customerName.trim().isEmpty()) {
            throw new DomainValidationException("Customer name or business name is required");
        }
        if (lines == null || lines.isEmpty()) {
            throw new DomainValidationException("Invoice must contain at least one line item");
        }

        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = tenantId;
        this.workOrderId = workOrderId;
        this.voucherType = voucherType;
        this.series = series;
        this.correlative = correlative;
        this.customerDocument = customerDocument.trim();
        this.customerName = customerName.trim();
        this.currency = currency != null ? currency : Currency.PEN;
        this.lines = new ArrayList<>(lines);

        calculateTotals();

        this.sunatStatus = sunatStatus != null ? sunatStatus : InvoiceStatus.DRAFT;
        this.pdfUrl = pdfUrl;
        this.qrContent = qrContent;
        this.voidReason = voidReason;
        this.issuedAt = issuedAt != null ? issuedAt : Instant.now();
        this.voidedAt = voidedAt;
    }

    private static void validateCustomerDocument(VoucherType voucherType, String doc) {
        if (voucherType == VoucherType.FACTURA) {
            new Ruc(doc);
        } else {
            if (doc.length() == 8) {
                new Dni(doc);
            } else if (doc.length() == 11) {
                new Ruc(doc);
            } else {
                throw new DomainValidationException("For Boleta the customer document must be an 8-digit DNI or an 11-digit RUC");
            }
        }
    }

    public void calculateTotals() {
        BigDecimal sumTotal = BigDecimal.ZERO;
        for (InvoiceLine line : lines) {
            sumTotal = sumTotal.add(line.getSubtotal());
        }
        this.totalAmount = sumTotal.setScale(2, RoundingMode.HALF_UP);
        this.totalTaxable = this.totalAmount.divide(IGV_DIVISOR, 2, RoundingMode.HALF_UP);
        this.totalIgv = this.totalAmount.subtract(this.totalTaxable).setScale(2, RoundingMode.HALF_UP);
    }

    public void markPendingSunat() {
        if (this.sunatStatus == InvoiceStatus.VOIDED) {
            throw new DomainValidationException("Cannot send a voided invoice to SUNAT");
        }
        this.sunatStatus = InvoiceStatus.PENDING_SUNAT;
    }

    public void markAccepted(String pdfUrl, String qrContent) {
        if (this.sunatStatus == InvoiceStatus.VOIDED) {
            throw new DomainValidationException("Cannot accept a previously voided invoice");
        }
        this.sunatStatus = InvoiceStatus.ACCEPTED;
        this.pdfUrl = pdfUrl;
        this.qrContent = qrContent;
    }

    public void markObserved() {
        if (this.sunatStatus == InvoiceStatus.VOIDED) {
            throw new DomainValidationException("Cannot mark a voided invoice as observed");
        }
        this.sunatStatus = InvoiceStatus.OBSERVED;
    }

    public void voidInvoice(String reason) {
        if (this.sunatStatus == InvoiceStatus.VOIDED) {
            throw new DomainValidationException("Invoice is already voided");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new DomainValidationException("Void reason is required");
        }
        this.sunatStatus = InvoiceStatus.VOIDED;
        this.voidReason = reason.trim();
        this.voidedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getWorkOrderId() {
        return workOrderId;
    }

    public VoucherType getVoucherType() {
        return voucherType;
    }

    public Series getSeries() {
        return series;
    }

    public Correlative getCorrelative() {
        return correlative;
    }

    public String getCustomerDocument() {
        return customerDocument;
    }

    public String getCustomerName() {
        return customerName;
    }

    public Currency getCurrency() {
        return currency;
    }

    public List<InvoiceLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public BigDecimal getTotalTaxable() {
        return totalTaxable;
    }

    public BigDecimal getTotalIgv() {
        return totalIgv;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public InvoiceStatus getSunatStatus() {
        return sunatStatus;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public String getQrContent() {
        return qrContent;
    }

    public String getVoidReason() {
        return voidReason;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getVoidedAt() {
        return voidedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Invoice invoice)) return false;
        return Objects.equals(id, invoice.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
