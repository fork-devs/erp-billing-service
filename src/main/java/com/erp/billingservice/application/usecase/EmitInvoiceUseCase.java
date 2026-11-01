package com.erp.billingservice.application.usecase;

import com.erp.billingservice.domain.model.Currency;
import com.erp.billingservice.domain.model.Invoice;
import com.erp.billingservice.domain.model.VoucherType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface EmitInvoiceUseCase {

    Invoice emitInvoice(EmitInvoiceCommand command);

    record EmitInvoiceCommand(
            UUID workOrderId,
            VoucherType voucherType,
            String series,
            String customerDocument,
            String customerName,
            Currency currency,
            List<InvoiceItemCommand> items
    ) {}

    record InvoiceItemCommand(
            String code,
            String description,
            BigDecimal quantity,
            BigDecimal unitPrice,
            String affectationType
    ) {}
}
