package com.erp.billingservice.domain.port;

import com.erp.billingservice.domain.model.Quote;
import com.erp.billingservice.domain.model.QuoteStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuoteRepositoryPort {
    Quote save(Quote quote);
    Optional<Quote> findById(UUID quoteId);
    Optional<Quote> findByWorkOrderId(UUID workOrderId);
    List<Quote> findByBranchId(UUID branchId, QuoteStatus status);
}
