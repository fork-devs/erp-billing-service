package com.erp.billingservice.application.usecase;

import com.erp.billingservice.domain.model.Invoice;
import com.erp.billingservice.domain.model.Payment;
import com.erp.billingservice.domain.model.VoucherType;

import java.util.UUID;

public interface ExecuteCheckoutUseCase {
    CheckoutResult executeDirectCheckout(DirectCheckoutCommand command);
    CheckoutResult executeMercadoPagoCheckout(MercadoPagoCheckoutCommand command);

    record DirectCheckoutCommand(
            UUID quoteId,
            VoucherType voucherType,
            String customerDocumentType,
            String customerDocumentNumber,
            String customerName,
            String method
    ) {}

    record MercadoPagoCheckoutCommand(
            UUID quoteId,
            VoucherType voucherType,
            String customerDocumentType,
            String customerDocumentNumber,
            String customerName,
            String paymentId
    ) {}

    record CheckoutResult(
            Invoice invoice,
            Payment payment
    ) {}
}
