package com.erp.billingservice.infrastructure.client.factos.dto;

import java.math.BigDecimal;

public record FactosItemDto(
        String code,
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice,
        String affectationType
) {
}
