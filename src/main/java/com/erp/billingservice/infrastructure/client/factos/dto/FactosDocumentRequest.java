package com.erp.billingservice.infrastructure.client.factos.dto;

import java.util.List;

public record FactosDocumentRequest(
        String series,
        String correlative,
        String cpeType,
        String issueDate,
        String issuerRuc,
        String acquirerDocument,
        String acquirerName,
        String currency,
        List<FactosItemDto> items
) {
}
