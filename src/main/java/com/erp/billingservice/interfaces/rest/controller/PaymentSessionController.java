package com.erp.billingservice.interfaces.rest.controller;

import com.erp.billingservice.application.service.PaymentSessionService;
import com.erp.billingservice.domain.port.PaymentSessionStrategy;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentSessionController {

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
            String sandboxCheckoutUrl,
            String currency,
            String externalReference,
            Instant createdAt
    ) {
        public static PaymentSessionResponse from(PaymentSessionStrategy.PaymentSessionResult result) {
            String normalizedProvider = "MERCADOPAGO".equalsIgnoreCase(result.provider()) ? "MERCADO_PAGO" : result.provider();
            String extRef = result.quoteId() != null ? result.quoteId().toString() : (result.invoiceId() != null ? result.invoiceId().toString() : result.sessionId().toString());
            return new PaymentSessionResponse(
                    result.sessionId(),
                    normalizedProvider,
                    result.quoteId(),
                    result.invoiceId(),
                    result.status(),
                    result.checkoutUrl(),
                    result.checkoutUrl() != null ? result.checkoutUrl().replace("www.mercadopago.com.pe", "sandbox.mercadopago.com.pe") : null,
                    "PEN",
                    extRef,
                    result.createdAt()
            );
        }
    }

    private final PaymentSessionService paymentSessionService;

    public PaymentSessionController(PaymentSessionService paymentSessionService) {
        this.paymentSessionService = paymentSessionService;
    }

    @PostMapping("/sessions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentSessionResponse> createPaymentSession(
            @Valid @RequestBody CreatePaymentSessionRequest request) {
        PaymentSessionStrategy.PaymentSessionResult result = paymentSessionService.createSession(
                request.provider(),
                request.quoteId(),
                request.invoiceId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(PaymentSessionResponse.from(result));
    }
}
