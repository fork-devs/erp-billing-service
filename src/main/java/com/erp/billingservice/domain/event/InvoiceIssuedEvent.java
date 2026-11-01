package com.erp.billingservice.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InvoiceIssuedEvent(
        UUID eventId,
        UUID tenantId,
        UUID invoiceId,
        UUID workOrderId,
        String voucherType,
        String series,
        int correlative,
        String customerDocument,
        String customerName,
        BigDecimal totalAmount,
        String currency,
        Instant occurredAt
) {
    public static InvoiceIssuedEvent of(UUID tenantId, UUID invoiceId, UUID workOrderId,
                                       String voucherType, String series, int correlative,
                                       String customerDocument, String customerName,
                                       BigDecimal totalAmount, String currency) {
        return new InvoiceIssuedEvent(
                UUID.randomUUID(), tenantId, invoiceId, workOrderId, voucherType,
                series, correlative, customerDocument, customerName, totalAmount, currency, Instant.now()
        );
    }
}
