package com.erp.billingservice.application.service;

import com.erp.billingservice.domain.exception.DomainValidationException;
import com.erp.billingservice.domain.exception.QuoteNotFoundException;
import com.erp.billingservice.domain.model.Quote;
import com.erp.billingservice.domain.model.QuoteItem;
import com.erp.billingservice.domain.model.QuoteItemType;
import com.erp.billingservice.domain.model.QuoteStatus;
import com.erp.billingservice.domain.port.OutboxRepositoryPort;
import com.erp.billingservice.domain.port.QuoteRepositoryPort;
import com.erp.billingservice.domain.port.WorkOrderClientPort;
import com.erp.billingservice.infrastructure.persistence.rls.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManageQuoteServiceTest {

    @Mock
    private QuoteRepositoryPort quoteRepository;

    @Mock
    private WorkOrderClientPort workOrderClient;

    @Mock
    private OutboxRepositoryPort outboxRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private QuoteApplicationService quoteService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID branchId = UUID.randomUUID();
    private final UUID quoteId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(tenantId);
        quoteService = new QuoteApplicationService(quoteRepository, workOrderClient, outboxRepository, objectMapper);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("Should get quote by ID or throw not found")
    void shouldGetQuoteById() {
        Quote quote = Quote.create(tenantId, UUID.randomUUID(), branchId, BigDecimal.ZERO, null, List.of());
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quote));

        Quote result = quoteService.getQuote(quoteId);
        assertThat(result).isEqualTo(quote);

        when(quoteRepository.findById(quoteId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> quoteService.getQuote(quoteId))
                .isInstanceOf(QuoteNotFoundException.class);
    }

    @Test
    @DisplayName("Should list quotes by branch and status")
    void shouldListQuotes() {
        Quote quote = Quote.create(tenantId, UUID.randomUUID(), branchId, BigDecimal.ZERO, null, List.of());
        when(quoteRepository.findByBranchId(branchId, QuoteStatus.DRAFT)).thenReturn(List.of(quote));

        List<Quote> results = quoteService.listQuotes(branchId, QuoteStatus.DRAFT);
        assertThat(results).hasSize(1);
    }

    @Test
    @DisplayName("Should update discount on DRAFT quote")
    void shouldUpdateDiscount() {
        QuoteItem item = new QuoteItem(UUID.randomUUID(), tenantId, null, QuoteItemType.SERVICE,
                "Oil", BigDecimal.ONE, new BigDecimal("100.00"), null);
        Quote quote = Quote.create(tenantId, UUID.randomUUID(), branchId, BigDecimal.ZERO, null, List.of(item));

        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quote));
        when(quoteRepository.save(any(Quote.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Quote updated = quoteService.updateDiscount(quoteId, new BigDecimal("20.00"), "Discount note");

        assertThat(updated.getDiscountPercentage()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(updated.getTotalAmount()).isEqualByComparingTo(new BigDecimal("80.00"));
        assertThat(updated.getNotes()).isEqualTo("Discount note");
    }

    @Test
    @DisplayName("Should approve DRAFT quote and save outbox event")
    void shouldApproveQuote() {
        Quote quote = Quote.create(tenantId, UUID.randomUUID(), branchId, BigDecimal.ZERO, null, List.of());
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quote));
        when(quoteRepository.save(any(Quote.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Quote approved = quoteService.approveQuote(quoteId);

        assertThat(approved.getStatus()).isEqualTo(QuoteStatus.APPROVED);
        assertThat(approved.getApprovedAt()).isNotNull();

        verify(outboxRepository).save(eq(tenantId), eq(approved.getId()), any(UUID.class), eq("QuoteApproved"), anyString());
    }

    @Test
    @DisplayName("Should cancel quote and save outbox event")
    void shouldCancelQuote() {
        Quote quote = Quote.create(tenantId, UUID.randomUUID(), branchId, BigDecimal.ZERO, null, List.of());
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quote));
        when(quoteRepository.save(any(Quote.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Quote canceled = quoteService.cancelQuote(quoteId);

        assertThat(canceled.getStatus()).isEqualTo(QuoteStatus.CANCELED);
        assertThat(canceled.getCanceledAt()).isNotNull();

        verify(outboxRepository).save(eq(tenantId), eq(canceled.getId()), any(UUID.class), eq("QuoteCanceled"), anyString());
    }
}
