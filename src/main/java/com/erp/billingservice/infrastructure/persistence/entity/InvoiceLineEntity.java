package com.erp.billingservice.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "invoice_lines")
public class InvoiceLineEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private InvoiceEntity invoice;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "affectation_type", nullable = false, length = 30)
    private String affectationType;

    public InvoiceLineEntity() {
    }

    public InvoiceLineEntity(UUID id, UUID tenantId, InvoiceEntity invoice, String code,
                             String description, BigDecimal quantity, BigDecimal unitPrice,
                             BigDecimal subtotal, String affectationType) {
        this.id = id;
        this.tenantId = tenantId;
        this.invoice = invoice;
        this.code = code;
        this.description = description;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = subtotal;
        this.affectationType = affectationType;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public InvoiceEntity getInvoice() {
        return invoice;
    }

    public void setInvoice(InvoiceEntity invoice) {
        this.invoice = invoice;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public String getAffectationType() {
        return affectationType;
    }

    public void setAffectationType(String affectationType) {
        this.affectationType = affectationType;
    }
}
