package com.erp.billingservice.infrastructure.client.factos.dto;

import java.math.BigDecimal;
import java.util.List;

public record FactosDocumentResponse(
        String series,
        String correlative,
        String cpeType,
        String issueDate,
        String issuerRuc,
        String acquirerDocument,
        String acquirerName,
        String status,
        BigDecimal totalTaxable,
        BigDecimal totalIgv,
        BigDecimal totalAmount,
        String currency,
        String pdfUrl,
        List<FactosItemDto> items
) {
}
