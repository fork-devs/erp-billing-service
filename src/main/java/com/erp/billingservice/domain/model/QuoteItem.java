package com.erp.billingservice.domain.model;

import com.erp.billingservice.domain.exception.DomainValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

public class QuoteItem {
    private final UUID id;
    private final UUID tenantId;
    private final UUID quoteId;
    private final QuoteItemType type;
    private final String name;
    private final BigDecimal quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal total;

    public QuoteItem(UUID id, UUID tenantId, UUID quoteId, QuoteItemType type,
                     String name, BigDecimal quantity, BigDecimal unitPrice, BigDecimal total) {
        if (tenantId == null) {
            throw new DomainValidationException("Tenant ID is required for quote item");
        }
        if (type == null) {
            throw new DomainValidationException("Quote item type is required");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new DomainValidationException("Quote item name is required");
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainValidationException("Quantity must be greater than zero");
        }
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new DomainValidationException("Unit price cannot be negative");
        }

        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = tenantId;
        this.quoteId = quoteId;
        this.type = type;
        this.name = name.trim();
        this.quantity = quantity.setScale(2, RoundingMode.HALF_UP);
        this.unitPrice = unitPrice.setScale(2, RoundingMode.HALF_UP);
        this.total = total != null ? total.setScale(2, RoundingMode.HALF_UP)
                : this.quantity.multiply(this.unitPrice).setScale(2, RoundingMode.HALF_UP);
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getQuoteId() {
        return quoteId;
    }

    public QuoteItemType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getTotal() {
        return total;
    }
}
