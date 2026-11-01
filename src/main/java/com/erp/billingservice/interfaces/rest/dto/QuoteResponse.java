package com.erp.billingservice.interfaces.rest.dto;

import com.erp.billingservice.domain.model.Quote;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record QuoteResponse(
        UUID id,
        UUID workOrderId,
        UUID branchId,
        BigDecimal subtotalAmount,
        BigDecimal discountPercentage,
        BigDecimal totalAmount,
        String status,
        String notes,
        Instant createdAt,
        Instant approvedAt,
        Instant canceledAt,
        List<QuoteItemResponse> items
) {
    public static QuoteResponse from(Quote quote) {
        List<QuoteItemResponse> itemResponses = quote.getItems() != null
                ? quote.getItems().stream().map(QuoteItemResponse::from).toList()
                : List.of();

        return new QuoteResponse(
                quote.getId(),
                quote.getWorkOrderId(),
                quote.getBranchId(),
                quote.getSubtotalAmount(),
                quote.getDiscountPercentage(),
                quote.getTotalAmount(),
                quote.getStatus().name(),
                quote.getNotes(),
                quote.getCreatedAt(),
                quote.getApprovedAt(),
                quote.getCanceledAt(),
                itemResponses
        );
    }
}
