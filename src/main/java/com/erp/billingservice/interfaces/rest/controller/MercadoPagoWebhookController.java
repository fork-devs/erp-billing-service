package com.erp.billingservice.interfaces.rest.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments/mercadopago")
public class MercadoPagoWebhookController {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoWebhookController.class);

    @PostMapping("/webhooks")
    public ResponseEntity<Void> receiveWebhook(
            @RequestHeader(value = "x-signature", required = false) String signature,
            @RequestHeader(value = "x-request-id", required = false) String requestId,
            @RequestParam(value = "id", required = false) String paymentId,
            @RequestParam(value = "topic", required = false) String topic,
            @RequestBody(required = false) String payload
    ) {
        log.info("Received Mercado Pago webhook: requestId={}, paymentId={}, topic={}", requestId, paymentId, topic);
        return ResponseEntity.ok().build();
    }
}
