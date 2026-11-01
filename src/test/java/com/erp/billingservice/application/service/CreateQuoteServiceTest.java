package com.erp.billingservice.application.service;

import com.erp.billingservice.application.usecase.CreateQuoteUseCase;
import com.erp.billingservice.domain.exception.DomainValidationException;
import com.erp.billingservice.domain.exception.QuoteConflictException;
import com.erp.billingservice.domain.exception.QuoteNotFoundException;
import com.erp.billingservice.domain.model.Quote;
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
class CreateQuoteServiceTest {

    @Mock
    private QuoteRepositoryPort quoteRepository;

    @Mock
    private WorkOrderClientPort workOrderClient;

    @Mock
    private OutboxRepositoryPort outboxRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private QuoteApplicationService quoteService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID workOrderId = UUID.randomUUID();
    private final UUID branchId = UUID.randomUUID();

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
    @DisplayName("Should create quote successfully from completed work order")
    void shouldCreateQuoteSuccessfully() {
        when(quoteRepository.findByWorkOrderId(workOrderId)).thenReturn(Optional.empty());

        WorkOrderClientPort.WorkOrderItem item1 = new WorkOrderClientPort.WorkOrderItem(
                "SERVICE", "Brake inspection", new BigDecimal("1.00"), new BigDecimal("80.00"), new BigDecimal("80.00")
        );
        WorkOrderClientPort.WorkOrderItem item2 = new WorkOrderClientPort.WorkOrderItem(
                "PRODUCT", "Brake pads", new BigDecimal("2.00"), new BigDecimal("85.00"), new BigDecimal("170.00")
        );

        WorkOrderClientPort.WorkOrderDetail workOrder = new WorkOrderClientPort.WorkOrderDetail(
                workOrderId, "OT-001", "COMPLETED", branchId, UUID.randomUUID(),
                "John Doe", "12345678", List.of(item1, item2)
        );
        when(workOrderClient.getWorkOrder(workOrderId)).thenReturn(Optional.of(workOrder));
        when(quoteRepository.save(any(Quote.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateQuoteUseCase.CreateQuoteCommand command = new CreateQuoteUseCase.CreateQuoteCommand(
                workOrderId, branchId, new BigDecimal("10.00"), "10% promo discount"
        );

        Quote result = quoteService.createQuote(command);

        assertThat(result).isNotNull();
        assertThat(result.getTenantId()).isEqualTo(tenantId);
        assertThat(result.getWorkOrderId()).isEqualTo(workOrderId);
        assertThat(result.getBranchId()).isEqualTo(branchId);
        assertThat(result.getStatus()).isEqualTo(QuoteStatus.DRAFT);
        assertThat(result.getSubtotalAmount()).isEqualByComparingTo(new BigDecimal("250.00"));
        assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("225.00"));

        verify(quoteRepository).save(any(Quote.class));
        verify(outboxRepository).save(eq(tenantId), eq(result.getId()), any(UUID.class), eq("QuoteCreated"), anyString());
    }

    @Test
    @DisplayName("Should reject quote creation if work order already has quote")
    void shouldRejectWhenQuoteAlreadyExists() {
        when(quoteRepository.findByWorkOrderId(workOrderId)).thenReturn(Optional.of(mock(Quote.class)));

        CreateQuoteUseCase.CreateQuoteCommand command = new CreateQuoteUseCase.CreateQuoteCommand(
                workOrderId, branchId, BigDecimal.ZERO, null
        );

        assertThatThrownBy(() -> quoteService.createQuote(command))
                .isInstanceOf(QuoteConflictException.class)
                .hasMessageContaining("already has an associated quote");

        verifyNoInteractions(workOrderClient);
    }

    @Test
    @DisplayName("Should reject quote creation if work order is not found")
    void shouldRejectWhenWorkOrderNotFound() {
        when(quoteRepository.findByWorkOrderId(workOrderId)).thenReturn(Optional.empty());
        when(workOrderClient.getWorkOrder(workOrderId)).thenReturn(Optional.empty());

        CreateQuoteUseCase.CreateQuoteCommand command = new CreateQuoteUseCase.CreateQuoteCommand(
                workOrderId, branchId, BigDecimal.ZERO, null
        );

        assertThatThrownBy(() -> quoteService.createQuote(command))
                .isInstanceOf(QuoteNotFoundException.class)
                .hasMessageContaining("Work order not found");
    }

    @Test
    @DisplayName("Should reject quote creation if work order is not completed")
    void shouldRejectWhenWorkOrderNotCompleted() {
        when(quoteRepository.findByWorkOrderId(workOrderId)).thenReturn(Optional.empty());

        WorkOrderClientPort.WorkOrderDetail workOrder = new WorkOrderClientPort.WorkOrderDetail(
                workOrderId, "OT-001", "IN_PROGRESS", branchId, UUID.randomUUID(),
                "John Doe", "12345678", List.of()
        );
        when(workOrderClient.getWorkOrder(workOrderId)).thenReturn(Optional.of(workOrder));

        CreateQuoteUseCase.CreateQuoteCommand command = new CreateQuoteUseCase.CreateQuoteCommand(
                workOrderId, branchId, BigDecimal.ZERO, null
        );

        assertThatThrownBy(() -> quoteService.createQuote(command))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Work order must be COMPLETED");
    }
}
