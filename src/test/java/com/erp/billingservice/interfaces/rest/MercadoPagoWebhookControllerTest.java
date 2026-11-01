package com.erp.billingservice.interfaces.rest;

import com.erp.billingservice.interfaces.rest.controller.MercadoPagoWebhookController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MercadoPagoWebhookControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new MercadoPagoWebhookController()).build();
    }

    @Test
    @DisplayName("POST /api/v1/payments/mercadopago/webhooks returns 200 OK")
    void shouldAcceptMercadoPagoWebhook() throws Exception {
        mockMvc.perform(post("/api/v1/payments/mercadopago/webhooks")
                        .header("x-signature", "ts=123,v1=abc")
                        .header("x-request-id", "req-123")
                        .param("id", "payment-999")
                        .param("topic", "payment")
                        .content("{\"data\":{\"id\":\"payment-999\"}}"))
                .andExpect(status().isOk());
    }
}
