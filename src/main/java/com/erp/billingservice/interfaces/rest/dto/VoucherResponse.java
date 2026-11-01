package com.erp.billingservice.interfaces.rest.dto;

import com.erp.billingservice.domain.model.Invoice;
import com.erp.billingservice.domain.model.InvoiceStatus;
import com.erp.billingservice.domain.model.Payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VoucherResponse(
        UUID id,
        UUID quoteId,
        String type,
        String customerDocumentType,
        String customerDocumentNumber,
        String customerName,
        BigDecimal totalAmount,
        String status,
        String externalInvoiceId,
        String series,
        String correlative,
        String fiscalNumber,
        String pdfUrl,
        List<PaymentItem> payments,
        BigDecimal totalPaid,
        BigDecimal balance
) {
    public record PaymentItem(
            UUID id,
            BigDecimal amount,
            String method,
            Instant paidAt
    ) {
    }

    public static VoucherResponse from(Invoice invoice, List<Payment> paymentsList) {
        BigDecimal totalPaid = paymentsList.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal balance = invoice.getTotalAmount().subtract(totalPaid);
        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            balance = BigDecimal.ZERO;
        }

        String status;
        if (invoice.getSunatStatus() == InvoiceStatus.VOIDED) {
            status = "CANCELED";
        } else if (totalPaid.compareTo(invoice.getTotalAmount()) >= 0 && invoice.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
            status = "PAID";
        } else if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {
            status = "PARTIALLY_PAID";
        } else {
            status = "PENDING";
        }

        String correlativeStr = String.format("%08d", invoice.getCorrelative().getValue());
        String seriesStr = invoice.getSeries().getValue();
        String fiscalNumber = seriesStr + "-" + correlativeStr;

        String docType = (invoice.getCustomerDocument() != null && invoice.getCustomerDocument().length() == 11) ? "RUC" : "DNI";

        List<PaymentItem> paymentItems = paymentsList.stream()
                .map(p -> new PaymentItem(
                        p.getId(),
                        p.getAmount(),
                        p.getProvider(),
                        p.getCreatedAt()
                ))
                .toList();

        return new VoucherResponse(
                invoice.getId(),
                invoice.getQuoteId(),
                invoice.getVoucherType().name(),
                docType,
                invoice.getCustomerDocument(),
                invoice.getCustomerName(),
                invoice.getTotalAmount(),
                status,
                invoice.getId().toString(),
                seriesStr,
                correlativeStr,
                fiscalNumber,
                invoice.getPdfUrl(),
                paymentItems,
                totalPaid,
                balance
        );
    }
}
