package com.erp.billingservice.application.service;

import com.erp.billingservice.application.usecase.RegisterPaymentUseCase;
import com.erp.billingservice.domain.event.PaymentReceivedEvent;
import com.erp.billingservice.domain.exception.InvoiceNotFoundException;
import com.erp.billingservice.domain.exception.TenantContextMissingException;
import com.erp.billingservice.domain.model.Payment;
import com.erp.billingservice.domain.model.PaymentStatus;
import com.erp.billingservice.domain.port.InvoiceRepositoryPort;
import com.erp.billingservice.domain.port.OutboxRepositoryPort;
import com.erp.billingservice.domain.port.PaymentRepositoryPort;
import com.erp.billingservice.infrastructure.persistence.rls.TenantContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentApplicationService implements RegisterPaymentUseCase {

    private static final Logger log = LoggerFactory.getLogger(PaymentApplicationService.class);

    private final PaymentRepositoryPort paymentRepository;
    private final InvoiceRepositoryPort invoiceRepository;
    private final OutboxRepositoryPort outboxRepository;
    private final ObjectMapper objectMapper;

    public PaymentApplicationService(
            PaymentRepositoryPort paymentRepository,
            InvoiceRepositoryPort invoiceRepository,
            OutboxRepositoryPort outboxRepository,
            ObjectMapper objectMapper
    ) {
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public Payment registerPayment(RegisterPaymentCommand command) {
        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new TenantContextMissingException("Tenant context is required to register a payment");
        }

        // Idempotency check: if key already processed for this tenant, return cached result
        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(tenantId, command.idempotencyKey());
        if (existing.isPresent()) {
            log.info("Idempotent request detected for key {}. Returning existing payment {}",
                    command.idempotencyKey(), existing.get().getId());
            return existing.get();
        }

        // Verify invoice exists
        invoiceRepository.findById(command.invoiceId())
                .orElseThrow(() -> new InvoiceNotFoundException(command.invoiceId()));

        Payment payment = new Payment(
                UUID.randomUUID(),
                tenantId,
                command.invoiceId(),
                command.provider(),
                command.providerPaymentId(),
                command.idempotencyKey(),
                command.amount(),
                command.currency(),
                PaymentStatus.CONFIRMED,
                Instant.now()
        );

        Payment saved = paymentRepository.save(payment);

        PaymentReceivedEvent event = PaymentReceivedEvent.of(
                tenantId,
                saved.getId(),
                saved.getInvoiceId(),
                saved.getProvider(),
                saved.getIdempotencyKey(),
                saved.getAmount(),
                saved.getCurrency().name()
        );

        try {
            String json = objectMapper.writeValueAsString(event);
            outboxRepository.save(tenantId, saved.getId(), event.eventId(), "PaymentReceived", json);
        } catch (Exception e) {
            log.error("Failed to serialize PaymentReceived event", e);
            throw new RuntimeException("Error persisting outbox event", e);
        }

        return saved;
    }
}
