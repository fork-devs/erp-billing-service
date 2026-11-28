package com.erp.billingservice.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_intents")
public class PaymentIntentJpaEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "quote_id", nullable = false)
    private UUID quoteId;

    @Column(name = "preference_id", nullable = false, length = 120)
    private String preferenceId;

    @Column(name = "external_reference", nullable = false, length = 120)
    private String externalReference;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "voucher_type", length = 20)
    private String voucherType;

    @Column(name = "customer_document_type", length = 20)
    private String customerDocumentType;

    @Column(name = "customer_document_number", length = 30)
    private String customerDocumentNumber;

    @Column(name = "customer_name", length = 255)
    private String customerName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public PaymentIntentJpaEntity() {}

    public PaymentIntentJpaEntity(UUID id, UUID tenantId, UUID quoteId, String preferenceId,
                                  String externalReference, BigDecimal amount, String currency,
                                  String status, String voucherType, String customerDocumentType,
                                  String customerDocumentNumber, String customerName,
                                  Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.quoteId = quoteId;
        this.preferenceId = preferenceId;
        this.externalReference = externalReference;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.voucherType = voucherType;
        this.customerDocumentType = customerDocumentType;
        this.customerDocumentNumber = customerDocumentNumber;
        this.customerName = customerName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getQuoteId() { return quoteId; }
    public String getPreferenceId() { return preferenceId; }
    public String getExternalReference() { return externalReference; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getVoucherType() { return voucherType; }
    public String getCustomerDocumentType() { return customerDocumentType; }
    public String getCustomerDocumentNumber() { return customerDocumentNumber; }
    public String getCustomerName() { return customerName; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
