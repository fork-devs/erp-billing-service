package com.erp.billingservice.interfaces.rest.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateQuoteRequest(
        @NotNull(message = "Work order ID is required")
        UUID workOrderId,

        @NotNull(message = "Branch ID is required")
        UUID branchId,

        @DecimalMin(value = "0.0", message = "Discount percentage cannot be negative")
        @DecimalMax(value = "100.0", message = "Discount percentage cannot exceed 100")
        BigDecimal discountPercentage,

        String notes
) {
    public CreateQuoteRequest {
        if (discountPercentage == null) {
            discountPercentage = BigDecimal.ZERO;
        }
    }
}
