package com.erp.billingservice.interfaces.rest.dto;

import com.erp.billingservice.domain.model.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record RegisterPaymentRequest(
        @NotNull(message = "{validation.billing.payment.invoice-id.not-null}")
        UUID invoiceId,

        @NotBlank(message = "{validation.billing.payment.provider.not-blank}")
        String provider,

        String providerPaymentId,

        @NotNull(message = "{validation.billing.payment.amount.positive}")
        @DecimalMin(value = "0.01", message = "{validation.billing.payment.amount.positive}")
        BigDecimal amount,

        Currency currency
) {
}
