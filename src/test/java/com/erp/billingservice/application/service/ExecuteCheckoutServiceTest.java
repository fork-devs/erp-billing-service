package com.erp.billingservice.application.service;

import com.erp.billingservice.application.usecase.ExecuteCheckoutUseCase;
import com.erp.billingservice.domain.exception.QuoteConflictException;
import com.erp.billingservice.domain.exception.QuoteNotFoundException;
import com.erp.billingservice.domain.model.*;
import com.erp.billingservice.domain.port.*;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExecuteCheckoutServiceTest {

    @Mock
    private QuoteRepositoryPort quoteRepository;

    @Mock
    private InvoiceRepositoryPort invoiceRepository;

    @Mock
    private PaymentRepositoryPort paymentRepository;

    @Mock
    private FiscalProviderPort fiscalProvider;

    @Mock
    private MercadoPagoClientPort mercadoPagoClient;

    @Mock
    private OutboxRepositoryPort outboxRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private CheckoutApplicationService checkoutService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID quoteId = UUID.randomUUID();
    private final UUID workOrderId = UUID.randomUUID();
    private final UUID branchId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(tenantId);
        checkoutService = new CheckoutApplicationService(
                quoteRepository, invoiceRepository, paymentRepository, fiscalProvider,
                mercadoPagoClient, outboxRepository, objectMapper, "20123456789"
        );
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("Should execute direct in-person checkout for approved quote")
    void shouldExecuteDirectCheckout() {
        QuoteItem item = new QuoteItem(UUID.randomUUID(), tenantId, null, QuoteItemType.SERVICE,
                "Brake service", BigDecimal.ONE, new BigDecimal("200.00"), null);
        Quote quote = Quote.create(tenantId, workOrderId, branchId, BigDecimal.ZERO, null, List.of(item));
        quote.approve();

        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quote));
        when(invoiceRepository.findByQuoteId(quoteId)).thenReturn(Optional.empty());
        when(invoiceRepository.getNextCorrelative(eq(tenantId), anyString())).thenReturn(1);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FiscalEmissionResult fiscalResult = new FiscalEmissionResult("ACCEPTED", "https://pdf.url", "qr-data");
        when(fiscalProvider.emitDocument(any(Invoice.class), anyString())).thenReturn(fiscalResult);

        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExecuteCheckoutUseCase.DirectCheckoutCommand command = new ExecuteCheckoutUseCase.DirectCheckoutCommand(
                quoteId, VoucherType.BOLETA, "DNI", "48765432", "Maria Fernandez", "CASH"
        );

        ExecuteCheckoutUseCase.CheckoutResult result = checkoutService.executeDirectCheckout(command);

        assertThat(result).isNotNull();
        assertThat(result.invoice()).isNotNull();
        assertThat(result.invoice().getQuoteId()).isEqualTo(quote.getId());
        assertThat(result.invoice().getSunatStatus()).isEqualTo(InvoiceStatus.ACCEPTED);
        assertThat(result.invoice().getPdfUrl()).isEqualTo("https://pdf.url");
        assertThat(result.payment()).isNotNull();
        assertThat(result.payment().getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
        assertThat(result.payment().getProvider()).isEqualTo("CASH");

        verify(outboxRepository, times(2)).save(eq(tenantId), any(UUID.class), any(UUID.class), anyString(), anyString());
    }

    @Test
    @DisplayName("Should reject direct checkout if quote is not approved")
    void shouldRejectDirectCheckoutIfDraft() {
        Quote quote = Quote.create(tenantId, workOrderId, branchId, BigDecimal.ZERO, null, List.of());
        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quote));

        ExecuteCheckoutUseCase.DirectCheckoutCommand command = new ExecuteCheckoutUseCase.DirectCheckoutCommand(
                quoteId, VoucherType.BOLETA, "DNI", "48765432", "Maria Fernandez", "CASH"
        );

        assertThatThrownBy(() -> checkoutService.executeDirectCheckout(command))
                .isInstanceOf(QuoteConflictException.class)
                .hasMessageContaining("is not APPROVED");
    }

    @Test
    @DisplayName("Should execute Mercado Pago checkout with verification")
    void shouldExecuteMercadoPagoCheckout() {
        QuoteItem item = new QuoteItem(UUID.randomUUID(), tenantId, null, QuoteItemType.SERVICE,
                "Inspection", BigDecimal.ONE, new BigDecimal("150.00"), null);
        Quote quote = Quote.create(tenantId, workOrderId, branchId, BigDecimal.ZERO, null, List.of(item));
        quote.approve();

        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quote));
        when(invoiceRepository.findByQuoteId(quoteId)).thenReturn(Optional.empty());

        MercadoPagoClientPort.PaymentVerification mpVerification = new MercadoPagoClientPort.PaymentVerification(
                "mp-12345", "approved", new BigDecimal("150.00"), "credit_card", "PEN"
        );
        when(mercadoPagoClient.verifyPayment("mp-12345")).thenReturn(Optional.of(mpVerification));

        when(invoiceRepository.getNextCorrelative(eq(tenantId), anyString())).thenReturn(2);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FiscalEmissionResult fiscalResult = new FiscalEmissionResult("ACCEPTED", "https://pdf.url", "qr-data");
        when(fiscalProvider.emitDocument(any(Invoice.class), anyString())).thenReturn(fiscalResult);

        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExecuteCheckoutUseCase.MercadoPagoCheckoutCommand command = new ExecuteCheckoutUseCase.MercadoPagoCheckoutCommand(
                quoteId, VoucherType.BOLETA, "DNI", "48765432", "Maria Fernandez", "mp-12345"
        );

        ExecuteCheckoutUseCase.CheckoutResult result = checkoutService.executeMercadoPagoCheckout(command);

        assertThat(result.invoice()).isNotNull();
        assertThat(result.payment()).isNotNull();
        assertThat(result.payment().getProvider()).isEqualTo("MERCADO_PAGO");
        assertThat(result.payment().getProviderPaymentId()).isEqualTo("mp-12345");
    }

    @Test
    @DisplayName("Should be idempotent on duplicate Mercado Pago checkout")
    void shouldReturnExistingOnDuplicateMercadoPagoCheckout() {
        Quote quote = Quote.create(tenantId, workOrderId, branchId, BigDecimal.ZERO, null, List.of());
        quote.approve();

        when(quoteRepository.findById(quoteId)).thenReturn(Optional.of(quote));

        Invoice existingInvoice = mock(Invoice.class);
        Payment existingPayment = mock(Payment.class);

        when(invoiceRepository.findByQuoteId(quoteId)).thenReturn(Optional.of(existingInvoice));
        when(paymentRepository.findByIdempotencyKey(tenantId, "mp-12345")).thenReturn(Optional.of(existingPayment));

        ExecuteCheckoutUseCase.MercadoPagoCheckoutCommand command = new ExecuteCheckoutUseCase.MercadoPagoCheckoutCommand(
                quoteId, VoucherType.BOLETA, "DNI", "48765432", "Maria Fernandez", "mp-12345"
        );

        ExecuteCheckoutUseCase.CheckoutResult result = checkoutService.executeMercadoPagoCheckout(command);

        assertThat(result.invoice()).isEqualTo(existingInvoice);
        assertThat(result.payment()).isEqualTo(existingPayment);
        verifyNoInteractions(mercadoPagoClient);
    }
}
