package com.erp.billingservice.domain.model;

import com.erp.billingservice.domain.exception.DomainValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Quote {
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    private final UUID id;
    private final UUID tenantId;
    private final UUID workOrderId;
    private final UUID branchId;
    private final List<QuoteItem> items;
    private String notes;

    private BigDecimal subtotalAmount;
    private BigDecimal discountPercentage;
    private BigDecimal totalAmount;

    private QuoteStatus status;
    private final Instant createdAt;
    private Instant approvedAt;
    private Instant canceledAt;

    public Quote(UUID id, UUID tenantId, UUID workOrderId, UUID branchId,
                 BigDecimal discountPercentage, String notes, List<QuoteItem> items,
                 QuoteStatus status, Instant createdAt, Instant approvedAt, Instant canceledAt) {
        if (tenantId == null) {
            throw new DomainValidationException("Tenant ID is required for quote");
        }
        if (workOrderId == null) {
            throw new DomainValidationException("Work order ID is required for quote");
        }
        if (branchId == null) {
            throw new DomainValidationException("Branch ID is required for quote");
        }
        if (discountPercentage != null && (discountPercentage.compareTo(BigDecimal.ZERO) < 0 || discountPercentage.compareTo(HUNDRED) > 0)) {
            throw new DomainValidationException("Discount percentage must be between 0 and 100");
        }

        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = tenantId;
        this.workOrderId = workOrderId;
        this.branchId = branchId;
        this.discountPercentage = discountPercentage != null ? discountPercentage.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.notes = notes;
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
        this.status = status != null ? status : QuoteStatus.DRAFT;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.approvedAt = approvedAt;
        this.canceledAt = canceledAt;

        calculateTotals();
    }

    public static Quote create(UUID tenantId, UUID workOrderId, UUID branchId,
                               BigDecimal discountPercentage, String notes, List<QuoteItem> items) {
        return new Quote(UUID.randomUUID(), tenantId, workOrderId, branchId, discountPercentage,
                notes, items, QuoteStatus.DRAFT, Instant.now(), null, null);
    }

    public void calculateTotals() {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (QuoteItem item : items) {
            subtotal = subtotal.add(item.getTotal());
        }
        this.subtotalAmount = subtotal.setScale(2, RoundingMode.HALF_UP);

        BigDecimal discountFactor = BigDecimal.ONE.subtract(this.discountPercentage.divide(HUNDRED, 4, RoundingMode.HALF_UP));
        this.totalAmount = this.subtotalAmount.multiply(discountFactor).setScale(2, RoundingMode.HALF_UP);
    }

    public void updateDiscount(BigDecimal newDiscountPercentage, String newNotes) {
        if (this.status != QuoteStatus.DRAFT) {
            throw new DomainValidationException("Cannot update discount on quote with status: " + this.status);
        }
        if (newDiscountPercentage == null || newDiscountPercentage.compareTo(BigDecimal.ZERO) < 0 || newDiscountPercentage.compareTo(HUNDRED) > 0) {
            throw new DomainValidationException("Discount percentage must be between 0 and 100");
        }
        this.discountPercentage = newDiscountPercentage.setScale(2, RoundingMode.HALF_UP);
        if (newNotes != null) {
            this.notes = newNotes;
        }
        calculateTotals();
    }

    public void approve() {
        if (this.status != QuoteStatus.DRAFT) {
            throw new DomainValidationException("Only DRAFT quotes can be approved, current status: " + this.status);
        }
        this.status = QuoteStatus.APPROVED;
        this.approvedAt = Instant.now();
    }

    public void cancel() {
        if (this.status == QuoteStatus.CANCELED) {
            throw new DomainValidationException("Quote is already canceled");
        }
        this.status = QuoteStatus.CANCELED;
        this.canceledAt = Instant.now();
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

    public UUID getBranchId() {
        return branchId;
    }

    public List<QuoteItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public String getNotes() {
        return notes;
    }

    public BigDecimal getSubtotalAmount() {
        return subtotalAmount;
    }

    public BigDecimal getDiscountPercentage() {
        return discountPercentage;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public QuoteStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public Instant getCanceledAt() {
        return canceledAt;
    }
}
