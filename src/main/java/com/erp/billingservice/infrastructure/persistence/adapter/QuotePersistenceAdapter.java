package com.erp.billingservice.infrastructure.persistence.adapter;

import com.erp.billingservice.domain.model.Quote;
import com.erp.billingservice.domain.model.QuoteStatus;
import com.erp.billingservice.domain.port.QuoteRepositoryPort;
import com.erp.billingservice.infrastructure.persistence.entity.QuoteEntity;
import com.erp.billingservice.infrastructure.persistence.mapper.QuoteEntityMapper;
import com.erp.billingservice.infrastructure.persistence.repository.SpringDataQuoteRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class QuotePersistenceAdapter implements QuoteRepositoryPort {

    private final SpringDataQuoteRepository repository;

    public QuotePersistenceAdapter(SpringDataQuoteRepository repository) {
        this.repository = repository;
    }

    @Override
    public Quote save(Quote quote) {
        QuoteEntity entity = QuoteEntityMapper.toEntity(quote);
        QuoteEntity saved = repository.save(entity);
        return QuoteEntityMapper.toDomain(saved);
    }

    @Override
    public Optional<Quote> findById(UUID quoteId) {
        return repository.findById(quoteId).map(QuoteEntityMapper::toDomain);
    }

    @Override
    public Optional<Quote> findByWorkOrderId(UUID workOrderId) {
        return repository.findByWorkOrderId(workOrderId).map(QuoteEntityMapper::toDomain);
    }

    @Override
    public List<Quote> findByBranchId(UUID branchId, QuoteStatus status) {
        List<QuoteEntity> entities = (status != null)
                ? repository.findByBranchIdAndStatus(branchId, status.name())
                : repository.findByBranchId(branchId);
        return entities.stream().map(QuoteEntityMapper::toDomain).toList();
    }
}
