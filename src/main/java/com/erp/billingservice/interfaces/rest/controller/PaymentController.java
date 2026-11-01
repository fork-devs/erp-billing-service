package com.erp.billingservice.interfaces.rest.controller;

import com.erp.billingservice.application.usecase.RegisterPaymentUseCase;
import com.erp.billingservice.domain.model.Currency;
import com.erp.billingservice.domain.model.Payment;
import com.erp.billingservice.domain.port.PaymentRepositoryPort;
import com.erp.billingservice.interfaces.rest.dto.PaymentResponse;
import com.erp.billingservice.interfaces.rest.dto.RegisterPaymentRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/billing/payments")
@Validated
public class PaymentController {

    private final RegisterPaymentUseCase registerPaymentUseCase;
    private final PaymentRepositoryPort paymentRepositoryPort;

    public PaymentController(
            RegisterPaymentUseCase registerPaymentUseCase,
            PaymentRepositoryPort paymentRepositoryPort
    ) {
        this.registerPaymentUseCase = registerPaymentUseCase;
        this.paymentRepositoryPort = paymentRepositoryPort;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'WORKSHOP_ADMIN', 'ADMIN_TALLER', 'SERVICE_ADVISOR', 'ASESOR')")
    public ResponseEntity<PaymentResponse> registerPayment(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody RegisterPaymentRequest request
    ) {
        RegisterPaymentUseCase.RegisterPaymentCommand command = new RegisterPaymentUseCase.RegisterPaymentCommand(
                request.invoiceId(),
                request.provider(),
                request.providerPaymentId(),
                idempotencyKey,
                request.amount(),
                request.currency() != null ? request.currency() : Currency.PEN
        );

        Payment payment = registerPaymentUseCase.registerPayment(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(PaymentResponse.fromDomain(payment));
    }

    @GetMapping("/invoice/{invoiceId}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'WORKSHOP_ADMIN', 'ADMIN_TALLER', 'SERVICE_ADVISOR', 'ASESOR', 'TECHNICIAN', 'MECANICO')")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByInvoiceId(@PathVariable UUID invoiceId) {
        List<Payment> payments = paymentRepositoryPort.findByInvoiceId(invoiceId);
        List<PaymentResponse> responses = payments.stream()
                .map(PaymentResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(responses);
    }
}
