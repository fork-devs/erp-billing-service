package com.erp.billingservice.application.usecase;

import com.erp.billingservice.domain.model.Quote;
import com.erp.billingservice.domain.model.QuoteStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ManageQuoteUseCase {
    Quote getQuote(UUID quoteId);
    List<Quote> listQuotes(UUID branchId, QuoteStatus status);
    Quote updateDiscount(UUID quoteId, BigDecimal discountPercentage, String notes);
    Quote approveQuote(UUID quoteId);
    Quote cancelQuote(UUID quoteId);
}
