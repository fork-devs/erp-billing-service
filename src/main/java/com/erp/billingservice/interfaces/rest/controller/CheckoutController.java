package com.erp.billingservice.interfaces.rest.controller;

import com.erp.billingservice.application.usecase.ExecuteCheckoutUseCase;
import com.erp.billingservice.interfaces.rest.dto.CheckoutResponse;
import com.erp.billingservice.interfaces.rest.dto.DirectCheckoutRequest;
import com.erp.billingservice.interfaces.rest.dto.MercadoPagoCheckoutRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/checkouts")
@Validated
public class CheckoutController {

    private final ExecuteCheckoutUseCase executeCheckoutUseCase;

    public CheckoutController(ExecuteCheckoutUseCase executeCheckoutUseCase) {
        this.executeCheckoutUseCase = executeCheckoutUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'WORKSHOP_ADMIN', 'ADMIN_TALLER', 'SERVICE_ADVISOR', 'ASESOR')")
    public ResponseEntity<CheckoutResponse> directCheckout(@Valid @RequestBody DirectCheckoutRequest request) {
        validateTaxDocument(request.customerDocumentType(), request.customerDocumentNumber());
        ExecuteCheckoutUseCase.DirectCheckoutCommand command = new ExecuteCheckoutUseCase.DirectCheckoutCommand(
                request.quoteId(),
                request.resolvedVoucherType(),
                request.customerDocumentType(),
                request.customerDocumentNumber(),
                request.customerName(),
                request.method()
        );

        ExecuteCheckoutUseCase.CheckoutResult result = executeCheckoutUseCase.executeDirectCheckout(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(CheckoutResponse.from(result));
    }

    @PostMapping("/mercadopago")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'WORKSHOP_ADMIN', 'ADMIN_TALLER', 'SERVICE_ADVISOR', 'ASESOR')")
    public ResponseEntity<CheckoutResponse> mercadoPagoCheckout(@Valid @RequestBody MercadoPagoCheckoutRequest request) {
        validateTaxDocument(request.customerDocumentType(), request.customerDocumentNumber());
        ExecuteCheckoutUseCase.MercadoPagoCheckoutCommand command = new ExecuteCheckoutUseCase.MercadoPagoCheckoutCommand(
                request.quoteId(),
                request.resolvedVoucherType(),
                request.customerDocumentType(),
                request.customerDocumentNumber(),
                request.customerName(),
                request.paymentId()
        );

        ExecuteCheckoutUseCase.CheckoutResult result = executeCheckoutUseCase.executeMercadoPagoCheckout(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(CheckoutResponse.from(result));
    }

    private static void validateTaxDocument(String docType, String docNumber) {
        if (docType == null || docNumber == null) {
            return;
        }
        String type = docType.trim().toUpperCase();
        String num = docNumber.trim();
        if ("DNI".equals(type) && !num.matches("\\d{8}")) {
            throw new com.erp.billingservice.domain.exception.DomainValidationException("DNI must have exactly 8 digits");
        }
        if ("RUC".equals(type) && !num.matches("\\d{11}")) {
            throw new com.erp.billingservice.domain.exception.DomainValidationException("RUC must have exactly 11 digits");
        }
    }
}
