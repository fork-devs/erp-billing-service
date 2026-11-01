package com.erp.billingservice.interfaces.rest;

import com.erp.billingservice.application.usecase.GetInvoiceUseCase;
import com.erp.billingservice.application.usecase.RegisterPaymentUseCase;
import com.erp.billingservice.domain.model.*;
import com.erp.billingservice.domain.port.InvoiceRepositoryPort;
import com.erp.billingservice.domain.port.PaymentRepositoryPort;
import com.erp.billingservice.interfaces.rest.advice.GlobalRestExceptionHandler;
import com.erp.billingservice.interfaces.rest.controller.VoucherController;
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
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class VoucherControllerTest {

    private MockMvc mockMvc;

    @Mock
    private InvoiceRepositoryPort invoiceRepository;

    @Mock
    private GetInvoiceUseCase getInvoiceUseCase;

    @Mock
    private RegisterPaymentUseCase registerPaymentUseCase;

    @Mock
    private PaymentRepositoryPort paymentRepository;

    @InjectMocks
    private VoucherController voucherController;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID voucherId = UUID.randomUUID();
    private final UUID quoteId = UUID.randomUUID();
    private final UUID workOrderId = UUID.randomUUID();
    private final UUID paymentId = UUID.randomUUID();

    private Invoice testInvoice;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(voucherController)
                .setControllerAdvice(new GlobalRestExceptionHandler())
                .build();

        InvoiceLine line = new InvoiceLine(UUID.randomUUID(), tenantId, "S1", "Service", BigDecimal.ONE, new BigDecimal("225.00"), "TAXABLE_ONEROUS");
        testInvoice = new Invoice(
                voucherId, tenantId, workOrderId, quoteId, VoucherType.BOLETA,
                new Series("B001"), new Correlative(1), "48765432", "Maria Fernandez",
                Currency.PEN, List.of(line), InvoiceStatus.ACCEPTED, "https://factos.com/pdf",
                "QR", null, Instant.now(), null
        );
    }

    @Test
    @DisplayName("GET /api/v1/vouchers/{voucherId} returns 200 OK with voucher and payments")
    void shouldGetVoucherById() throws Exception {
        when(invoiceRepository.findById(voucherId)).thenReturn(Optional.of(testInvoice));
        Payment payment = new Payment(paymentId, tenantId, voucherId, "CASH", "p-1", "idemp-1", new BigDecimal("225.00"), Currency.PEN, PaymentStatus.CONFIRMED, Instant.now());
        when(paymentRepository.findByInvoiceId(voucherId)).thenReturn(List.of(payment));

        mockMvc.perform(get("/api/v1/vouchers/{voucherId}", voucherId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(voucherId.toString()))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.totalPaid").value(225.00))
                .andExpect(jsonPath("$.balance").value(0.00));
    }

    @Test
    @DisplayName("POST /api/v1/vouchers/{voucherId}/payments registers payment and returns 201 Created")
    void shouldRegisterPaymentOnVoucher() throws Exception {
        when(invoiceRepository.findById(voucherId)).thenReturn(Optional.of(testInvoice));
        Payment payment = new Payment(paymentId, tenantId, voucherId, "CASH", null, "idemp-test", new BigDecimal("225.00"), Currency.PEN, PaymentStatus.CONFIRMED, Instant.now());
        when(registerPaymentUseCase.registerPayment(any())).thenReturn(payment);
        when(paymentRepository.findByInvoiceId(voucherId)).thenReturn(List.of(payment));

        String body = """
                {
                  "amount": 225.00,
                  "method": "CASH"
                }
                """;

        mockMvc.perform(post("/api/v1/vouchers/{voucherId}/payments", voucherId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(voucherId.toString()))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.totalPaid").value(225.00));
    }

    @Test
    @DisplayName("DELETE /api/v1/vouchers/{voucherId}/payments/{paymentId} deletes payment and returns 204 No Content")
    void shouldDeletePaymentFromVoucher() throws Exception {
        when(invoiceRepository.findById(voucherId)).thenReturn(Optional.of(testInvoice));
        Payment payment = new Payment(paymentId, tenantId, voucherId, "CASH", null, "idemp-test", new BigDecimal("225.00"), Currency.PEN, PaymentStatus.CONFIRMED, Instant.now());
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        mockMvc.perform(delete("/api/v1/vouchers/{voucherId}/payments/{paymentId}", voucherId, paymentId))
                .andExpect(status().isNoContent());

        verify(paymentRepository, times(1)).deleteById(paymentId);
    }
}
