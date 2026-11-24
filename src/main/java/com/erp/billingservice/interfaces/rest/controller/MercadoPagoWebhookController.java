package com.erp.billingservice.interfaces.rest.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Public webhook receiver for Mercado Pago notifications with HMAC-SHA256 signature verification.
 */
@RestController
@RequestMapping("/api/v1/payments/mercadopago")
public class MercadoPagoWebhookController {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoWebhookController.class);
    private final String webhookSecret;

    public MercadoPagoWebhookController() {
        this("");
    }

    @Autowired
    public MercadoPagoWebhookController(
            @Value("${MERCADOPAGO_WEBHOOK_SECRET:}") String webhookSecret
    ) {
        this.webhookSecret = webhookSecret != null ? webhookSecret.trim() : "";
    }

    @PostMapping("/webhooks")
    public ResponseEntity<Void> receiveWebhook(
            @RequestHeader(value = "x-signature", required = false) String signature,
            @RequestHeader(value = "x-request-id", required = false) String requestId,
            @RequestParam(value = "data.id", required = false) String dataId,
            @RequestParam(value = "id", required = false) String paymentId,
            @RequestParam(value = "topic", required = false) String topic,
            @RequestBody(required = false) String payload
    ) {
        String resourceId = (dataId != null && !dataId.isBlank()) ? dataId : paymentId;

        if (!webhookSecret.isBlank() && !isValidSignature(signature, requestId, resourceId)) {
            log.warn("Rejected Mercado Pago webhook with invalid or missing x-signature: requestId={}, resourceId={}",
                    requestId, resourceId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("Received verified Mercado Pago webhook: requestId={}, resourceId={}, topic={}",
                requestId, resourceId, topic);
        return ResponseEntity.ok().build();
    }

    static boolean verifyHmacSignature(String secret, String signatureHeader, String requestId, String resourceId) {
        if (signatureHeader == null || signatureHeader.isBlank()) {
            return false;
        }
        String ts = null;
        String v1 = null;
        for (String part : signatureHeader.split(",")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length == 2) {
                if ("ts".equals(kv[0].trim())) {
                    ts = kv[1].trim();
                } else if ("v1".equals(kv[0].trim())) {
                    v1 = kv[1].trim();
                }
            }
        }
        if (ts == null || v1 == null || ts.isBlank() || v1.isBlank()) {
            return false;
        }

        StringBuilder manifest = new StringBuilder();
        if (resourceId != null && !resourceId.isBlank()) {
            manifest.append("id:").append(resourceId).append(';');
        }
        if (requestId != null && !requestId.isBlank()) {
            manifest.append("request-id:").append(requestId).append(';');
        }
        manifest.append("ts:").append(ts).append(';');

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(manifest.toString().getBytes(StandardCharsets.UTF_8));
            String expected = HexFormat.of().formatHex(raw);
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    v1.toLowerCase().getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isValidSignature(String signatureHeader, String requestId, String resourceId) {
        return verifyHmacSignature(webhookSecret, signatureHeader, requestId, resourceId);
    }
}
