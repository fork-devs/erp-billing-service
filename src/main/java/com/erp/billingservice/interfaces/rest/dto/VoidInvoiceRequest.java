package com.erp.billingservice.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record VoidInvoiceRequest(
        @NotBlank(message = "{validation.billing.void.reason.not-blank}")
        String reason
) {
}
