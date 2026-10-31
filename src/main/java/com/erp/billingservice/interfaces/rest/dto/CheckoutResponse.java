package com.erp.billingservice.interfaces.rest.dto;

import com.erp.billingservice.application.usecase.ExecuteCheckoutUseCase;
import com.erp.billingservice.domain.model.Invoice;
import com.erp.billingservice.domain.model.Payment;

import java.math.BigDecimal;
import java.util.UUID;

public record CheckoutResponse(
        UUID id,
        UUID quoteId,
        UUID workOrderId,
        String voucherType,
        String series,
        int correlativo,
        String customerDocument,
        String customerName,
        BigDecimal totalAmount,
        String status,
        BigDecimal totalPaid,
        String pdfUrl,
        PaymentSummary payment
) {
    public static CheckoutResponse from(ExecuteCheckoutUseCase.CheckoutResult result) {
        Invoice invoice = result.invoice();
        Payment payment = result.payment();

        PaymentSummary summary = payment != null
                ? new PaymentSummary(payment.getId(), payment.getProvider(), payment.getAmount(), payment.getStatus().name())
                : null;

        return new CheckoutResponse(
                invoice.getId(),
                invoice.getQuoteId(),
                invoice.getWorkOrderId(),
                invoice.getVoucherType().name(),
                invoice.getSeries().getValue(),
                invoice.getCorrelative().getValue(),
                invoice.getCustomerDocument(),
                invoice.getCustomerName(),
                invoice.getTotalAmount(),
                "PAID",
                invoice.getTotalAmount(),
                invoice.getPdfUrl(),
                summary
        );
    }

    public record PaymentSummary(
            UUID id,
            String provider,
            BigDecimal amount,
            String status
    ) {
    }
}
