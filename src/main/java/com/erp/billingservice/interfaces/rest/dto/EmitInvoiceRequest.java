package com.erp.billingservice.interfaces.rest.dto;

import com.erp.billingservice.domain.model.Currency;
import com.erp.billingservice.domain.model.VoucherType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record EmitInvoiceRequest(
        @NotNull(message = "{validation.billing.work-order-id.not-null}")
        UUID workOrderId,

        @NotNull(message = "{validation.billing.voucher-type.not-null}")
        VoucherType voucherType,

        @NotBlank(message = "{validation.billing.series.not-blank}")
        String series,

        @NotBlank(message = "{validation.billing.customer-document.not-blank}")
        String customerDocument,

        @NotBlank(message = "{validation.billing.customer-name.not-blank}")
        String customerName,

        @NotNull(message = "{validation.billing.currency.not-null}")
        Currency currency,

        @NotEmpty(message = "{validation.billing.lines.not-empty}")
        @Valid
        List<InvoiceItemRequest> items
) {
}
