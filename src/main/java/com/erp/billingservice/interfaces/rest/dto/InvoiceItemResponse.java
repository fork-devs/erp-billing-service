package com.erp.billingservice.interfaces.rest.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record InvoiceItemResponse(
        UUID id,
        String code,
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal,
        String affectationType
) {
}
