package com.erp.billingservice.domain.model;

public record FiscalEmissionResult(
        String status,
        String pdfUrl,
        String qrContent
) {
}
