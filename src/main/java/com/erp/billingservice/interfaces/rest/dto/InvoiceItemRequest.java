package com.erp.billingservice.interfaces.rest.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record InvoiceItemRequest(
        @NotBlank(message = "{validation.billing.item.code.not-blank}")
        String code,

        @NotBlank(message = "{validation.billing.item.description.not-blank}")
        String description,

        @NotNull(message = "{validation.billing.item.quantity.positive}")
        @DecimalMin(value = "0.01", message = "{validation.billing.item.quantity.positive}")
        BigDecimal quantity,

        @NotNull(message = "{validation.billing.item.unit-price.not-null}")
        BigDecimal unitPrice,

        String affectationType
) {
}
