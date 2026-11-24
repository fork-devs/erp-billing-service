package com.erp.billingservice.interfaces.rest.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MercadoPagoWebhookControllerTest {

    private static final String SECRET = "test-webhook-secret-123";

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new MercadoPagoWebhookController(SECRET))
            .build();

    @Test
    @DisplayName("POST /api/v1/payments/mercadopago/webhooks returns 200 OK when x-signature is valid")
    void receiveWebhook_ValidSignature_Returns200() throws Exception {
        String requestId = "req-abc-123";
        String dataId = "987654321";
        String ts = "1732450000";
        String manifest = "id:" + dataId + ";request-id:" + requestId + ";ts:" + ts + ";";

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String v1 = HexFormat.of().formatHex(mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)));

        mockMvc.perform(post("/api/v1/payments/mercadopago/webhooks")
                        .param("data.id", dataId)
                        .param("topic", "payment")
                        .header("x-request-id", requestId)
                        .header("x-signature", "ts=" + ts + ",v1=" + v1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"payment.created\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/payments/mercadopago/webhooks returns 401 when x-signature is missing or invalid")
    void receiveWebhook_InvalidSignature_Returns401() throws Exception {
        mockMvc.perform(post("/api/v1/payments/mercadopago/webhooks")
                        .param("data.id", "987654321")
                        .header("x-request-id", "req-abc-123")
                        .header("x-signature", "ts=1732450000,v1=deadbeef")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"payment.created\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/payments/mercadopago/webhooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"payment.created\"}"))
                .andExpect(status().isUnauthorized());
    }
}
