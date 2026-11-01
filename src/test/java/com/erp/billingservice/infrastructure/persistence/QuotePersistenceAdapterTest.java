package com.erp.billingservice.infrastructure.persistence;

import com.erp.billingservice.domain.model.Quote;
import com.erp.billingservice.domain.model.QuoteItem;
import com.erp.billingservice.domain.model.QuoteItemType;
import com.erp.billingservice.domain.model.QuoteStatus;
import com.erp.billingservice.infrastructure.persistence.adapter.QuotePersistenceAdapter;
import com.erp.billingservice.infrastructure.persistence.entity.QuoteEntity;
import com.erp.billingservice.infrastructure.persistence.mapper.QuoteEntityMapper;
import com.erp.billingservice.infrastructure.persistence.repository.SpringDataQuoteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuotePersistenceAdapterTest {

    @Mock
    private SpringDataQuoteRepository repository;

    @InjectMocks
    private QuotePersistenceAdapter adapter;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID workOrderId = UUID.randomUUID();
    private final UUID branchId = UUID.randomUUID();
    private final UUID quoteId = UUID.randomUUID();

    @Test
    @DisplayName("Should save quote and map back to domain")
    void shouldSaveQuote() {
        QuoteItem item = new QuoteItem(UUID.randomUUID(), tenantId, null, QuoteItemType.SERVICE,
                "Oil change", BigDecimal.ONE, new BigDecimal("80.00"), null);
        Quote quote = Quote.create(tenantId, workOrderId, branchId, BigDecimal.ZERO, null, List.of(item));
        QuoteEntity entity = QuoteEntityMapper.toEntity(quote);

        when(repository.save(any(QuoteEntity.class))).thenReturn(entity);

        Quote saved = adapter.save(quote);

        assertThat(saved).isNotNull();
        assertThat(saved.getTenantId()).isEqualTo(tenantId);
        assertThat(saved.getWorkOrderId()).isEqualTo(workOrderId);
        verify(repository).save(any(QuoteEntity.class));
    }

    @Test
    @DisplayName("Should find quote by ID")
    void shouldFindById() {
        Quote quote = Quote.create(tenantId, workOrderId, branchId, BigDecimal.ZERO, null, List.of());
        QuoteEntity entity = QuoteEntityMapper.toEntity(quote);

        when(repository.findById(quoteId)).thenReturn(Optional.of(entity));

        Optional<Quote> result = adapter.findById(quoteId);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(quote.getId());
    }

    @Test
    @DisplayName("Should find quote by work order ID")
    void shouldFindByWorkOrderId() {
        Quote quote = Quote.create(tenantId, workOrderId, branchId, BigDecimal.ZERO, null, List.of());
        QuoteEntity entity = QuoteEntityMapper.toEntity(quote);

        when(repository.findByWorkOrderId(workOrderId)).thenReturn(Optional.of(entity));

        Optional<Quote> result = adapter.findByWorkOrderId(workOrderId);

        assertThat(result).isPresent();
        assertThat(result.get().getWorkOrderId()).isEqualTo(workOrderId);
    }

    @Test
    @DisplayName("Should find quotes by branch and status")
    void shouldFindByBranchIdAndStatus() {
        Quote quote = Quote.create(tenantId, workOrderId, branchId, BigDecimal.ZERO, null, List.of());
        QuoteEntity entity = QuoteEntityMapper.toEntity(quote);

        when(repository.findByBranchIdAndStatus(branchId, "DRAFT")).thenReturn(List.of(entity));

        List<Quote> results = adapter.findByBranchId(branchId, QuoteStatus.DRAFT);

        assertThat(results).hasSize(1);
    }
}
