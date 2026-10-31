package com.erp.billingservice.interfaces.rest;

import com.erp.billingservice.application.usecase.ExecuteCheckoutUseCase;
import com.erp.billingservice.domain.model.*;
import com.erp.billingservice.interfaces.rest.advice.GlobalRestExceptionHandler;
import com.erp.billingservice.interfaces.rest.controller.CheckoutController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CheckoutControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ExecuteCheckoutUseCase executeCheckoutUseCase;

    @InjectMocks
    private CheckoutController checkoutController;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID quoteId = UUID.randomUUID();
    private final UUID workOrderId = UUID.randomUUID();
    private final UUID invoiceId = UUID.randomUUID();
    private final UUID paymentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(checkoutController)
                .setControllerAdvice(new GlobalRestExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/billing/checkouts executes direct checkout and returns 201 Created")
    void shouldExecuteDirectCheckout() throws Exception {
        InvoiceLine line = new InvoiceLine(UUID.randomUUID(), tenantId, "S1", "Service", BigDecimal.ONE, new BigDecimal("200.00"), "TAXABLE_ONEROUS");
        Invoice invoice = new Invoice(
                invoiceId, tenantId, workOrderId, quoteId, VoucherType.BOLETA,
                new Series("B001"), new Correlative(1), "48765432", "Maria Fernandez",
                Currency.PEN, List.of(line), InvoiceStatus.ACCEPTED, "https://pdf.url",
                "qr-content", null, Instant.now(), null
        );
        Payment payment = new Payment(
                paymentId, tenantId, invoiceId, "CASH", null, "checkout-" + quoteId,
                new BigDecimal("200.00"), Currency.PEN, PaymentStatus.CONFIRMED, Instant.now()
        );

        ExecuteCheckoutUseCase.CheckoutResult result = new ExecuteCheckoutUseCase.CheckoutResult(invoice, payment);
        when(executeCheckoutUseCase.executeDirectCheckout(any(ExecuteCheckoutUseCase.DirectCheckoutCommand.class)))
                .thenReturn(result);

        String json = """
                {
                    "quoteId": "%s",
                    "type": "RECEIPT",
                    "customerDocumentType": "DNI",
                    "customerDocumentNumber": "48765432",
                    "customerName": "Maria Fernandez",
                    "method": "CASH"
                }
                """.formatted(quoteId);

        mockMvc.perform(post("/api/v1/billing/checkouts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quoteId").value(quoteId.toString()))
                .andExpect(jsonPath("$.voucherType").value("BOLETA"))
                .andExpect(jsonPath("$.series").value("B001"))
                .andExpect(jsonPath("$.correlativo").value(1))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.totalAmount").value(200.0))
                .andExpect(jsonPath("$.pdfUrl").value("https://pdf.url"))
                .andExpect(jsonPath("$.payment.provider").value("CASH"));
    }

    @Test
    @DisplayName("POST /api/v1/billing/checkouts/mercadopago executes Mercado Pago checkout")
    void shouldExecuteMercadoPagoCheckout() throws Exception {
        InvoiceLine line = new InvoiceLine(UUID.randomUUID(), tenantId, "S1", "Service", BigDecimal.ONE, new BigDecimal("150.00"), "TAXABLE_ONEROUS");
        Invoice invoice = new Invoice(
                invoiceId, tenantId, workOrderId, quoteId, VoucherType.FACTURA,
                new Series("F001"), new Correlative(1), "20123456789", "EMPRESA S.A.C.",
                Currency.PEN, List.of(line), InvoiceStatus.ACCEPTED, "https://pdf.url",
                "qr-content", null, Instant.now(), null
        );
        Payment payment = new Payment(
                paymentId, tenantId, invoiceId, "MERCADO_PAGO", "mp-999", "mp-mp-999",
                new BigDecimal("150.00"), Currency.PEN, PaymentStatus.CONFIRMED, Instant.now()
        );

        ExecuteCheckoutUseCase.CheckoutResult result = new ExecuteCheckoutUseCase.CheckoutResult(invoice, payment);
        when(executeCheckoutUseCase.executeMercadoPagoCheckout(any(ExecuteCheckoutUseCase.MercadoPagoCheckoutCommand.class)))
                .thenReturn(result);

        String json = """
                {
                    "quoteId": "%s",
                    "type": "INVOICE",
                    "customerDocumentType": "RUC",
                    "customerDocumentNumber": "20123456789",
                    "customerName": "EMPRESA S.A.C.",
                    "paymentId": "mp-999"
                }
                """.formatted(quoteId);

        mockMvc.perform(post("/api/v1/billing/checkouts/mercadopago")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.voucherType").value("FACTURA"))
                .andExpect(jsonPath("$.series").value("F001"))
                .andExpect(jsonPath("$.payment.provider").value("MERCADO_PAGO"));
    }
}
