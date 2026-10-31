package com.erp.billingservice.interfaces.rest.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpdateQuoteDiscountRequest(
        @NotNull(message = "Discount percentage is required")
        @DecimalMin(value = "0.0", message = "Discount percentage cannot be negative")
        @DecimalMax(value = "100.0", message = "Discount percentage cannot exceed 100")
        BigDecimal discountPercentage,

        String notes
) {
}
