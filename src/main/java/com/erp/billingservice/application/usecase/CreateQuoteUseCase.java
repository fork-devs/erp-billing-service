package com.erp.billingservice.application.usecase;

import com.erp.billingservice.domain.model.Quote;

import java.math.BigDecimal;
import java.util.UUID;

public interface CreateQuoteUseCase {
    Quote createQuote(CreateQuoteCommand command);

    record CreateQuoteCommand(
            UUID workOrderId,
            UUID branchId,
            BigDecimal discountPercentage,
            String notes
    ) {}
}
