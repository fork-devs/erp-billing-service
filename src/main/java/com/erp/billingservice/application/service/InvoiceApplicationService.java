package com.erp.billingservice.application.service;

import com.erp.billingservice.application.usecase.EmitInvoiceUseCase;
import com.erp.billingservice.application.usecase.GetInvoicePdfUseCase;
import com.erp.billingservice.application.usecase.GetInvoiceUseCase;
import com.erp.billingservice.application.usecase.VoidInvoiceUseCase;
import com.erp.billingservice.domain.event.InvoiceIssuedEvent;
import com.erp.billingservice.domain.event.InvoiceVoidedEvent;
import com.erp.billingservice.domain.exception.InvoiceConflictException;
import com.erp.billingservice.domain.exception.InvoiceNotFoundException;
import com.erp.billingservice.domain.exception.TenantContextMissingException;
import com.erp.billingservice.domain.model.Correlative;
import com.erp.billingservice.domain.model.FiscalEmissionResult;
import com.erp.billingservice.domain.model.Invoice;
import com.erp.billingservice.domain.model.InvoiceLine;
import com.erp.billingservice.domain.model.InvoiceStatus;
import com.erp.billingservice.domain.model.Series;
import com.erp.billingservice.domain.port.FiscalProviderPort;
import com.erp.billingservice.domain.port.InvoiceRepositoryPort;
import com.erp.billingservice.domain.port.OutboxRepositoryPort;
import com.erp.billingservice.infrastructure.persistence.rls.TenantContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class InvoiceApplicationService implements EmitInvoiceUseCase, GetInvoiceUseCase, VoidInvoiceUseCase, GetInvoicePdfUseCase {

    private static final Logger log = LoggerFactory.getLogger(InvoiceApplicationService.class);

    private final InvoiceRepositoryPort invoiceRepository;
    private final FiscalProviderPort fiscalProvider;
    private final OutboxRepositoryPort outboxRepository;
    private final ObjectMapper objectMapper;
    private final String defaultIssuerRuc;

    public InvoiceApplicationService(
            InvoiceRepositoryPort invoiceRepository,
            FiscalProviderPort fiscalProvider,
            OutboxRepositoryPort outboxRepository,
            ObjectMapper objectMapper,
            @Value("${erp.billing.issuer-ruc:20123456789}") String defaultIssuerRuc
    ) {
        this.invoiceRepository = invoiceRepository;
        this.fiscalProvider = fiscalProvider;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.defaultIssuerRuc = defaultIssuerRuc;
    }

    @Override
    @Transactional
    public Invoice emitInvoice(EmitInvoiceCommand command) {
        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new TenantContextMissingException("Tenant context is required to emit an invoice");
        }

        Optional<Invoice> existing = invoiceRepository.findByWorkOrderId(command.workOrderId());
        if (existing.isPresent()) {
            throw new InvoiceConflictException("Work order " + command.workOrderId() + " already has an associated invoice");
        }

        Series series = new Series(command.series());
        int nextCorrelative = invoiceRepository.getNextCorrelative(tenantId, series.getValue());
        Correlative correlative = new Correlative(nextCorrelative);

        List<InvoiceLine> lines = command.items().stream()
                .map(item -> new InvoiceLine(
                        UUID.randomUUID(),
                        tenantId,
                        item.code(),
                        item.description(),
                        item.quantity(),
                        item.unitPrice(),
                        item.affectationType()
                ))
                .toList();

        Invoice invoice = new Invoice(
                UUID.randomUUID(),
                tenantId,
                command.workOrderId(),
                command.voucherType(),
                series,
                correlative,
                command.customerDocument(),
                command.customerName(),
                command.currency(),
                lines,
                InvoiceStatus.DRAFT,
                null,
                null,
                null,
                null,
                null
        );

        Invoice saved = invoiceRepository.save(invoice);

        // Call fiscal provider (factos)
        FiscalEmissionResult fiscalResult = fiscalProvider.emitDocument(saved, defaultIssuerRuc);
        if ("ACCEPTED".equalsIgnoreCase(fiscalResult.status())) {
            saved.markAccepted(fiscalResult.pdfUrl(), fiscalResult.qrContent());
        } else {
            saved.markPendingSunat();
        }

        Invoice updated = invoiceRepository.save(saved);

        // Save domain event to outbox in same transaction
        InvoiceIssuedEvent event = InvoiceIssuedEvent.of(
                tenantId,
                updated.getId(),
                updated.getWorkOrderId(),
                updated.getVoucherType().name(),
                updated.getSeries().getValue(),
                updated.getCorrelative().getValue(),
                updated.getCustomerDocument(),
                updated.getCustomerName(),
                updated.getTotalAmount(),
                updated.getCurrency().name()
        );

        saveOutboxEvent(tenantId, updated.getId(), event.eventId(), "FacturaEmitida", event);

        return updated;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Invoice> getById(UUID id) {
        return invoiceRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Invoice> getByWorkOrderId(UUID workOrderId) {
        return invoiceRepository.findByWorkOrderId(workOrderId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Invoice> listInvoices(int page, int size) {
        return invoiceRepository.findAll(page, size);
    }

    @Override
    @Transactional
    public Invoice voidInvoice(UUID invoiceId, String reason) {
        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new TenantContextMissingException("Tenant context is required to void an invoice");
        }

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));

        invoice.voidInvoice(reason);
        Invoice saved = invoiceRepository.save(invoice);

        InvoiceVoidedEvent event = InvoiceVoidedEvent.of(
                tenantId,
                saved.getId(),
                saved.getSeries().getValue(),
                saved.getCorrelative().getValue(),
                reason
        );

        saveOutboxEvent(tenantId, saved.getId(), event.eventId(), "InvoiceVoided", event);

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getPdf(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));

        return fiscalProvider.downloadPdf(invoice.getSeries().getValue(), invoice.getCorrelative().toFormattedString());
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
