package com.erp.billingservice.application.service;

import com.erp.billingservice.application.usecase.CreateQuoteUseCase;
import com.erp.billingservice.application.usecase.ManageQuoteUseCase;
import com.erp.billingservice.domain.event.QuoteApprovedEvent;
import com.erp.billingservice.domain.event.QuoteCanceledEvent;
import com.erp.billingservice.domain.event.QuoteCreatedEvent;
import com.erp.billingservice.domain.exception.DomainValidationException;
import com.erp.billingservice.domain.exception.QuoteConflictException;
import com.erp.billingservice.domain.exception.QuoteNotFoundException;
import com.erp.billingservice.domain.exception.TenantContextMissingException;
import com.erp.billingservice.domain.model.Quote;
import com.erp.billingservice.domain.model.QuoteItem;
import com.erp.billingservice.domain.model.QuoteItemType;
import com.erp.billingservice.domain.model.QuoteStatus;
import com.erp.billingservice.domain.port.OutboxRepositoryPort;
import com.erp.billingservice.domain.port.QuoteRepositoryPort;
import com.erp.billingservice.domain.port.WorkOrderClientPort;
import com.erp.billingservice.infrastructure.persistence.rls.TenantContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class QuoteApplicationService implements CreateQuoteUseCase, ManageQuoteUseCase {

    private static final Logger log = LoggerFactory.getLogger(QuoteApplicationService.class);

    private final QuoteRepositoryPort quoteRepository;
    private final WorkOrderClientPort workOrderClient;
    private final OutboxRepositoryPort outboxRepository;
    private final ObjectMapper objectMapper;

    public QuoteApplicationService(
            QuoteRepositoryPort quoteRepository,
            WorkOrderClientPort workOrderClient,
            OutboxRepositoryPort outboxRepository,
            ObjectMapper objectMapper
    ) {
        this.quoteRepository = quoteRepository;
        this.workOrderClient = workOrderClient;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public Quote createQuote(CreateQuoteCommand command) {
        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new TenantContextMissingException("Tenant context is required to create a quote");
        }

        Optional<Quote> existing = quoteRepository.findByWorkOrderId(command.workOrderId());
        if (existing.isPresent()) {
            throw new QuoteConflictException("Work order " + command.workOrderId() + " already has an associated quote");
        }

        WorkOrderClientPort.WorkOrderDetail workOrder = workOrderClient.getWorkOrder(command.workOrderId())
                .orElseThrow(() -> new QuoteNotFoundException("Work order not found with ID: " + command.workOrderId()));

        if (!"COMPLETED".equalsIgnoreCase(workOrder.status()) && !"CLOSED".equalsIgnoreCase(workOrder.status())) {
            throw new DomainValidationException("Work order must be COMPLETED to generate quote, current status: " + workOrder.status());
        }

        List<QuoteItem> quoteItems = new ArrayList<>();
        if (workOrder.items() != null) {
            for (WorkOrderClientPort.WorkOrderItem item : workOrder.items()) {
                QuoteItemType type;
                try {
                    type = QuoteItemType.valueOf(item.type().toUpperCase());
                } catch (Exception e) {
                    type = QuoteItemType.SERVICE;
                }
                quoteItems.add(new QuoteItem(
                        UUID.randomUUID(),
                        tenantId,
                        null,
                        type,
                        item.description() != null ? item.description() : "Item",
                        item.quantity() != null ? item.quantity() : BigDecimal.ONE,
                        item.unitPrice() != null ? item.unitPrice() : BigDecimal.ZERO,
                        item.subtotal()
                ));
            }
        }

        Quote quote = Quote.create(
                tenantId,
                command.workOrderId(),
                command.branchId(),
                command.discountPercentage(),
                command.notes(),
                quoteItems
        );

        Quote saved = quoteRepository.save(quote);

        QuoteCreatedEvent event = QuoteCreatedEvent.of(
                saved.getTenantId(),
                saved.getId(),
                saved.getWorkOrderId(),
                saved.getBranchId(),
                saved.getSubtotalAmount(),
                saved.getDiscountPercentage(),
                saved.getTotalAmount()
        );
        saveOutboxEvent(saved.getTenantId(), saved.getId(), event.eventId(), "QuoteCreated", event);

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Quote getQuote(UUID quoteId) {
        return quoteRepository.findById(quoteId)
                .orElseThrow(() -> new QuoteNotFoundException(quoteId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Quote> listQuotes(UUID branchId, QuoteStatus status) {
        return quoteRepository.findByBranchId(branchId, status);
    }

    @Override
    @Transactional
    public Quote updateDiscount(UUID quoteId, BigDecimal discountPercentage, String notes) {
        Quote quote = getQuote(quoteId);
        quote.updateDiscount(discountPercentage, notes);
        return quoteRepository.save(quote);
    }

    @Override
    @Transactional
    public Quote approveQuote(UUID quoteId) {
        Quote quote = getQuote(quoteId);
        quote.approve();
        Quote saved = quoteRepository.save(quote);

        QuoteApprovedEvent event = QuoteApprovedEvent.of(
                saved.getTenantId(),
                saved.getId(),
                saved.getWorkOrderId(),
                saved.getBranchId(),
                saved.getTotalAmount()
        );
        saveOutboxEvent(saved.getTenantId(), saved.getId(), event.eventId(), "QuoteApproved", event);

        return saved;
    }

    @Override
    @Transactional
    public Quote cancelQuote(UUID quoteId) {
        Quote quote = getQuote(quoteId);
        quote.cancel();
        Quote saved = quoteRepository.save(quote);

        QuoteCanceledEvent event = QuoteCanceledEvent.of(
                saved.getTenantId(),
                saved.getId(),
                saved.getWorkOrderId(),
                saved.getBranchId()
        );
        saveOutboxEvent(saved.getTenantId(), saved.getId(), event.eventId(), "QuoteCanceled", event);

        return saved;
    }

    private void saveOutboxEvent(UUID tenantId, UUID aggregateId, UUID eventId, String eventType, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            outboxRepository.save(tenantId, aggregateId, eventId, eventType, json);
        } catch (Exception e) {
            log.error("Failed to serialize outbox event {}: {}", eventType, e.getMessage(), e);
            throw new RuntimeException("Error persisting outbox event", e);
        }
    }
}
