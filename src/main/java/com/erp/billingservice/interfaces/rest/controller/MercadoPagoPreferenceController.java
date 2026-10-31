package com.erp.billingservice.interfaces.rest.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments/mercadopago")
public class MercadoPagoPreferenceController {

    @PostMapping("/preferences")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'WORKSHOP_ADMIN', 'ADMIN_TALLER', 'SERVICE_ADVISOR', 'ASESOR')")
    public ResponseEntity<Map<String, Object>> createPreference(@RequestBody(required = false) Map<String, Object> request) {
        String quoteId = request != null && request.containsKey("quoteId")
                ? String.valueOf(request.get("quoteId"))
                : UUID.randomUUID().toString();
        String preferenceId = "pref-" + UUID.randomUUID();
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "preferenceId", preferenceId,
                "initPoint", "https://www.mercadopago.com.pe/checkout/v1/redirect?pref_id=" + preferenceId,
                "sandboxInitPoint", "https://sandbox.mercadopago.com.pe/checkout/v1/redirect?pref_id=" + preferenceId,
                "amount", new BigDecimal("225.00"),
                "currency", "PEN",
                "externalReference", quoteId
        ));
    }
}
