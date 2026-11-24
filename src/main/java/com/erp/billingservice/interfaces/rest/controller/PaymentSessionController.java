package com.erp.billingservice.interfaces.rest.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentSessionController {

    private static final Set<String> SUPPORTED_PROVIDERS = Set.of("MERCADOPAGO", "STRIPE", "CASH");

    public record CreatePaymentSessionRequest(
            UUID quoteId,
            UUID invoiceId,
            @NotBlank String provider
    ) {}

    public record PaymentSessionResponse(
            UUID sessionId,
            String provider,
            UUID quoteId,
            UUID invoiceId,
            String status,
            String checkoutUrl,
            Instant createdAt
    ) {}

    @PostMapping("/sessions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentSessionResponse> createPaymentSession(
            @Valid @RequestBody CreatePaymentSessionRequest request) {
        String normalizedProvider = request.provider().trim().toUpperCase();
        if (!SUPPORTED_PROVIDERS.contains(normalizedProvider)) {
            throw new IllegalArgumentException("Unsupported payment session provider: " + request.provider());
        }
        if (request.quoteId() == null && request.invoiceId() == null) {
            throw new IllegalArgumentException("Either quoteId or invoiceId is required");
        }
        UUID sessionId = UUID.randomUUID();
        String checkoutUrl = switch (normalizedProvider) {
            case "MERCADOPAGO" -> "https://www.mercadopago.com.pe/checkout/v1/redirect?pref_id=pref-" + sessionId;
            case "STRIPE" -> "https://checkout.stripe.com/c/pay/cs_" + sessionId;
            default -> null;
        };
        return ResponseEntity.status(HttpStatus.CREATED).body(new PaymentSessionResponse(
                sessionId,
                normalizedProvider,
                request.quoteId(),
                request.invoiceId(),
                "PENDING",
                checkoutUrl,
                Instant.now()
        ));
    }
}
