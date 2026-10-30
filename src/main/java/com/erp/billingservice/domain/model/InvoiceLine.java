package com.erp.billingservice.domain.model;

import com.erp.billingservice.domain.exception.DomainValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;

public class InvoiceLine {
    private final UUID id;
    private final UUID tenantId;
    private final String code;
    private final String description;
    private final BigDecimal quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal subtotal;
    private final String affectationType;

    public InvoiceLine(UUID id, UUID tenantId, String code, String description,
                       BigDecimal quantity, BigDecimal unitPrice, String affectationType) {
        if (code == null || code.trim().isEmpty()) {
            throw new DomainValidationException("Item code is required");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new DomainValidationException("Item description is required");
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainValidationException("Quantity must be greater than zero");
        }
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new DomainValidationException("Unit price cannot be negative");
        }

        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = tenantId;
        this.code = code.trim();
        this.description = description.trim();
        this.quantity = quantity.setScale(2, RoundingMode.HALF_UP);
        this.unitPrice = unitPrice.setScale(2, RoundingMode.HALF_UP);
        this.subtotal = this.quantity.multiply(this.unitPrice).setScale(2, RoundingMode.HALF_UP);
        this.affectationType = (affectationType != null && !affectationType.trim().isEmpty())
                ? affectationType.trim() : "TAXABLE_ONEROUS";
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public String getAffectationType() {
        return affectationType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InvoiceLine that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
