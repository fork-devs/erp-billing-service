package com.erp.billingservice.interfaces.rest.dto;

import com.erp.billingservice.domain.model.VoucherType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record MercadoPagoCheckoutRequest(
        @NotNull(message = "Quote ID is required")
        UUID quoteId,

        @NotNull(message = "Voucher type is required")
        String type,

        @NotBlank(message = "Customer document type is required")
        String customerDocumentType,

        @NotBlank(message = "Customer document number is required")
        String customerDocumentNumber,

        @NotBlank(message = "Customer name is required")
        String customerName,

        @NotBlank(message = "Payment ID is required")
        String paymentId
) {
    public VoucherType resolvedVoucherType() {
        if ("INVOICE".equalsIgnoreCase(type) || "FACTURA".equalsIgnoreCase(type)) {
            return VoucherType.FACTURA;
        }
        return VoucherType.BOLETA;
    }
}
