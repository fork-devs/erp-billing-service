package com.erp.billingservice.interfaces.rest.dto;

import com.erp.billingservice.domain.model.QuoteItem;

import java.math.BigDecimal;
import java.util.UUID;

public record QuoteItemResponse(
        UUID id,
        String type,
        String name,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal total
) {
    public static QuoteItemResponse from(QuoteItem item) {
        return new QuoteItemResponse(
                item.getId(),
                item.getType().name(),
                item.getName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getTotal()
        );
    }
}
