package com.erp.billingservice.interfaces.rest;

import com.erp.billingservice.application.usecase.EmitInvoiceUseCase;
import com.erp.billingservice.application.usecase.GetInvoicePdfUseCase;
import com.erp.billingservice.application.usecase.GetInvoiceUseCase;
import com.erp.billingservice.application.usecase.VoidInvoiceUseCase;
import com.erp.billingservice.infrastructure.config.I18nConfig;
import com.erp.billingservice.interfaces.rest.advice.GlobalRestExceptionHandler;
import com.erp.billingservice.interfaces.rest.controller.InvoiceController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class I18nValidationTest {

    private MockMvc mockMvc;

    @Mock
    private EmitInvoiceUseCase emitInvoiceUseCase;
    @Mock
    private GetInvoiceUseCase getInvoiceUseCase;
    @Mock
    private VoidInvoiceUseCase voidInvoiceUseCase;
    @Mock
    private GetInvoicePdfUseCase getInvoicePdfUseCase;

    private static final String INVALID_INVOICE_REQUEST = """
            {
              "workOrderId": "00000000-0000-0000-0000-000000000001",
              "voucherType": "FACTURA",
              "series": "",
              "customerDocument": "20123456789",
              "customerName": "EMPRESA S.A.C.",
              "currency": "PEN",
              "items": [
                {
                  "code": "SERV-01",
                  "description": "Reparacion",
                  "quantity": 1,
                  "unitPrice": 100.00
                }
              ]
            }
            """;

    @BeforeEach
    void setUp() {
        I18nConfig config = new I18nConfig();
        MessageSource messageSource = config.messageSource();
        LocalValidatorFactoryBean validator = config.getValidator(messageSource);
        validator.afterPropertiesSet();

        InvoiceController controller = new InvoiceController(
                emitInvoiceUseCase,
                getInvoiceUseCase,
                voidInvoiceUseCase,
                getInvoicePdfUseCase
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalRestExceptionHandler(messageSource))
                .setValidator(validator)
                .build();
    }

    @Test
    @DisplayName("Should return validation error message in Spanish when Accept-Language is es")
    void shouldReturnValidationMessageInSpanish() throws Exception {
        mockMvc.perform(post("/api/v1/billing/invoices")
                        .header("Accept-Language", "es")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INVALID_INVOICE_REQUEST))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("La serie del comprobante es obligatoria.")));
    }

    @Test
    @DisplayName("Should return validation error message in English when Accept-Language is en")
    void shouldReturnValidationMessageInEnglish() throws Exception {
        mockMvc.perform(post("/api/v1/billing/invoices")
                        .header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INVALID_INVOICE_REQUEST))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("Voucher series is required.")));
    }
}
