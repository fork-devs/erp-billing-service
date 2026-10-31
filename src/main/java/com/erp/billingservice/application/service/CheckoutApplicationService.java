package com.erp.billingservice.application.service;

import com.erp.billingservice.application.usecase.ExecuteCheckoutUseCase;
import com.erp.billingservice.domain.event.InvoiceIssuedEvent;
import com.erp.billingservice.domain.event.PaymentReceivedEvent;
import com.erp.billingservice.domain.exception.DomainValidationException;
import com.erp.billingservice.domain.exception.QuoteConflictException;
import com.erp.billingservice.domain.exception.QuoteNotFoundException;
import com.erp.billingservice.domain.exception.TenantContextMissingException;
import com.erp.billingservice.domain.model.Correlative;
import com.erp.billingservice.domain.model.Currency;
import com.erp.billingservice.domain.model.FiscalEmissionResult;
import com.erp.billingservice.domain.model.Invoice;
import com.erp.billingservice.domain.model.InvoiceLine;
import com.erp.billingservice.domain.model.InvoiceStatus;
import com.erp.billingservice.domain.model.Payment;
import com.erp.billingservice.domain.model.PaymentStatus;
import com.erp.billingservice.domain.model.Quote;
import com.erp.billingservice.domain.model.QuoteItem;
import com.erp.billingservice.domain.model.QuoteStatus;
import com.erp.billingservice.domain.model.Series;
import com.erp.billingservice.domain.model.VoucherType;
import com.erp.billingservice.domain.port.FiscalProviderPort;
import com.erp.billingservice.domain.port.InvoiceRepositoryPort;
import com.erp.billingservice.domain.port.MercadoPagoClientPort;
import com.erp.billingservice.domain.port.OutboxRepositoryPort;
import com.erp.billingservice.domain.port.PaymentRepositoryPort;
import com.erp.billingservice.domain.port.QuoteRepositoryPort;
import com.erp.billingservice.infrastructure.persistence.rls.TenantContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CheckoutApplicationService implements ExecuteCheckoutUseCase {

    private static final Logger log = LoggerFactory.getLogger(CheckoutApplicationService.class);

    private final QuoteRepositoryPort quoteRepository;
    private final InvoiceRepositoryPort invoiceRepository;
    private final PaymentRepositoryPort paymentRepository;
    private final FiscalProviderPort fiscalProvider;
    private final MercadoPagoClientPort mercadoPagoClient;
    private final OutboxRepositoryPort outboxRepository;
    private final ObjectMapper objectMapper;
    private final String defaultIssuerRuc;

    public CheckoutApplicationService(
            QuoteRepositoryPort quoteRepository,
            InvoiceRepositoryPort invoiceRepository,
            PaymentRepositoryPort paymentRepository,
            FiscalProviderPort fiscalProvider,
            MercadoPagoClientPort mercadoPagoClient,
            OutboxRepositoryPort outboxRepository,
            ObjectMapper objectMapper,
            @Value("${erp.billing.issuer-ruc:20123456789}") String defaultIssuerRuc
    ) {
        this.quoteRepository = quoteRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.fiscalProvider = fiscalProvider;
        this.mercadoPagoClient = mercadoPagoClient;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.defaultIssuerRuc = defaultIssuerRuc;
    }

    @Override
    @Transactional
    public CheckoutResult executeDirectCheckout(DirectCheckoutCommand command) {
        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new TenantContextMissingException("Tenant context is required for checkout");
        }

        Quote quote = quoteRepository.findById(command.quoteId())
                .orElseThrow(() -> new QuoteNotFoundException(command.quoteId()));

        if (quote.getStatus() != QuoteStatus.APPROVED) {
            throw new QuoteConflictException("Quote " + command.quoteId() + " is not APPROVED (status: " + quote.getStatus() + ")");
        }

        if (invoiceRepository.findByQuoteId(command.quoteId()).isPresent()) {
            throw new QuoteConflictException("Quote " + command.quoteId() + " already has an emitted invoice");
        }

        Invoice invoice = createAndEmitInvoice(tenantId, quote, command.voucherType(),
                command.customerDocumentNumber(), command.customerName());

        Payment payment = new Payment(
                UUID.randomUUID(),
                tenantId,
                invoice.getId(),
                command.method() != null ? command.method() : "CASH",
                null,
                "checkout-" + quote.getId(),
                invoice.getTotalAmount(),
                Currency.PEN,
                PaymentStatus.CONFIRMED,
                Instant.now()
        );
        Payment savedPayment = paymentRepository.save(payment);

        PaymentReceivedEvent paymentEvent = PaymentReceivedEvent.of(
                savedPayment.getTenantId(),
                savedPayment.getId(),
                savedPayment.getInvoiceId(),
                savedPayment.getProvider(),
                savedPayment.getIdempotencyKey(),
                savedPayment.getAmount(),
                savedPayment.getCurrency().name()
        );
        saveOutboxEvent(savedPayment.getTenantId(), savedPayment.getId(), paymentEvent.eventId(), "PaymentReceived", paymentEvent);

        return new CheckoutResult(invoice, savedPayment);
    }

    @Override
    @Transactional
    public CheckoutResult executeMercadoPagoCheckout(MercadoPagoCheckoutCommand command) {
        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new TenantContextMissingException("Tenant context is required for checkout");
        }

        Quote quote = quoteRepository.findById(command.quoteId())
                .orElseThrow(() -> new QuoteNotFoundException(command.quoteId()));

        if (quote.getStatus() != QuoteStatus.APPROVED) {
            throw new QuoteConflictException("Quote " + command.quoteId() + " is not APPROVED (status: " + quote.getStatus() + ")");
        }

        String idempotencyKey = command.paymentId().startsWith("mp-")
                ? command.paymentId()
                : "mp-" + command.paymentId();

        // Idempotency check: if invoice already exists for quote
        Optional<Invoice> existingInvoice = invoiceRepository.findByQuoteId(command.quoteId());
        if (existingInvoice.isPresent()) {
            Optional<Payment> existingPayment = paymentRepository.findByIdempotencyKey(tenantId, idempotencyKey);
            if (existingPayment.isPresent()) {
                return new CheckoutResult(existingInvoice.get(), existingPayment.get());
            }
            throw new QuoteConflictException("Quote " + command.quoteId() + " already has an emitted invoice");
        }

        // Verify with Mercado Pago API
        MercadoPagoClientPort.PaymentVerification verification = mercadoPagoClient.verifyPayment(command.paymentId())
                .orElseThrow(() -> new DomainValidationException("Mercado Pago payment not found: " + command.paymentId()));

        if (!"approved".equalsIgnoreCase(verification.status())) {
            throw new QuoteConflictException("Mercado Pago payment is not approved (status: " + verification.status() + ")");
        }

        if (verification.transactionAmount() == null ||
                verification.transactionAmount().compareTo(quote.getTotalAmount()) != 0) {
            throw new QuoteConflictException("Payment amount (" + verification.transactionAmount() +
                    ") does not match quote total (" + quote.getTotalAmount() + ")");
        }

        Invoice invoice = createAndEmitInvoice(tenantId, quote, command.voucherType(),
                command.customerDocumentNumber(), command.customerName());

        Payment payment = new Payment(
                UUID.randomUUID(),
                tenantId,
                invoice.getId(),
                "MERCADO_PAGO",
                command.paymentId(),
                idempotencyKey,
                invoice.getTotalAmount(),
                Currency.PEN,
                PaymentStatus.CONFIRMED,
                Instant.now()
        );
        Payment savedPayment = paymentRepository.save(payment);

        PaymentReceivedEvent paymentEvent = PaymentReceivedEvent.of(
                savedPayment.getTenantId(),
                savedPayment.getId(),
                savedPayment.getInvoiceId(),
                savedPayment.getProvider(),
                savedPayment.getIdempotencyKey(),
                savedPayment.getAmount(),
                savedPayment.getCurrency().name()
        );
        saveOutboxEvent(savedPayment.getTenantId(), savedPayment.getId(), paymentEvent.eventId(), "PaymentReceived", paymentEvent);

        return new CheckoutResult(invoice, savedPayment);
    }

    private Invoice createAndEmitInvoice(UUID tenantId, Quote quote, VoucherType voucherType,
                                         String customerDoc, String customerName) {
        String seriesStr = voucherType == VoucherType.FACTURA ? "F001" : "B001";
        Series series = new Series(seriesStr);
        int nextCorrelative = invoiceRepository.getNextCorrelative(tenantId, series.getValue());
        Correlative correlative = new Correlative(nextCorrelative);

        BigDecimal discountFactor = BigDecimal.ONE.subtract(
                quote.getDiscountPercentage().divide(new BigDecimal("100.00"), 4, RoundingMode.HALF_UP)
        );

        List<InvoiceLine> lines = new ArrayList<>();
        if (quote.getItems().isEmpty()) {
            lines.add(new InvoiceLine(
                    UUID.randomUUID(),
                    tenantId,
                    "SERVICE-01",
                    "Workshop services",
                    BigDecimal.ONE,
                    quote.getTotalAmount(),
                    "TAXABLE_ONEROUS"
            ));
        } else {
            for (QuoteItem item : quote.getItems()) {
                BigDecimal itemUnitPriceDiscounted = item.getUnitPrice().multiply(discountFactor).setScale(2, RoundingMode.HALF_UP);
                lines.add(new InvoiceLine(
                        UUID.randomUUID(),
                        tenantId,
                        item.getType().name() + "-" + item.getId().toString().substring(0, 8),
                        item.getName(),
                        item.getQuantity(),
                        itemUnitPriceDiscounted,
                        "TAXABLE_ONEROUS"
                ));
            }
        }

        Invoice invoice = new Invoice(
                UUID.randomUUID(),
                tenantId,
                quote.getWorkOrderId(),
                quote.getId(),
                voucherType,
                series,
                correlative,
                customerDoc,
                customerName,
                Currency.PEN,
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

        saved = invoiceRepository.save(saved);

        InvoiceIssuedEvent event = InvoiceIssuedEvent.of(
                saved.getTenantId(),
                saved.getId(),
                saved.getWorkOrderId(),
                saved.getVoucherType().name(),
                saved.getSeries().getValue(),
                saved.getCorrelative().getValue(),
                saved.getCustomerDocument(),
                saved.getCustomerName(),
                saved.getTotalAmount(),
                saved.getCurrency().name()
        );
        saveOutboxEvent(saved.getTenantId(), saved.getId(), event.eventId(), "InvoiceIssued", event);

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
