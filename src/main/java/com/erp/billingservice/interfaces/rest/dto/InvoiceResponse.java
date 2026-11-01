package com.erp.billingservice.interfaces.rest.dto;

import com.erp.billingservice.domain.model.Invoice;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        UUID tenantId,
        UUID workOrderId,
        String voucherType,
        String series,
        int correlativo,
        String customerDocument,
        String customerName,
        String currency,
        BigDecimal totalTaxable,
        BigDecimal totalIgv,
        BigDecimal totalAmount,
        String sunatStatus,
        String pdfUrl,
        String qrContent,
        Instant issuedAt,
        List<InvoiceItemResponse> items
) {
    public static InvoiceResponse fromDomain(Invoice invoice) {
        List<InvoiceItemResponse> itemResponses = invoice.getLines().stream()
                .map(line -> new InvoiceItemResponse(
                        line.getId(),
                        line.getCode(),
                        line.getDescription(),
                        line.getQuantity(),
                        line.getUnitPrice(),
                        line.getSubtotal(),
                        line.getAffectationType()
                ))
                .toList();

        return new InvoiceResponse(
                invoice.getId(),
                invoice.getTenantId(),
                invoice.getWorkOrderId(),
                invoice.getVoucherType().name(),
                invoice.getSeries().getValue(),
                invoice.getCorrelative().getValue(),
                invoice.getCustomerDocument(),
                invoice.getCustomerName(),
                invoice.getCurrency().name(),
                invoice.getTotalTaxable(),
                invoice.getTotalIgv(),
                invoice.getTotalAmount(),
                invoice.getSunatStatus().name(),
                invoice.getPdfUrl(),
                invoice.getQrContent(),
                invoice.getIssuedAt(),
                itemResponses
        );
    }
}
