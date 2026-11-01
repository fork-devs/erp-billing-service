package com.erp.billingservice.interfaces.rest.controller;

import com.erp.billingservice.application.usecase.CreateQuoteUseCase;
import com.erp.billingservice.application.usecase.ManageQuoteUseCase;
import com.erp.billingservice.domain.model.Quote;
import com.erp.billingservice.domain.model.QuoteStatus;
import com.erp.billingservice.interfaces.rest.dto.CreateQuoteRequest;
import com.erp.billingservice.interfaces.rest.dto.QuoteResponse;
import com.erp.billingservice.interfaces.rest.dto.UpdateQuoteDiscountRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/billing/quotes", "/api/v1/quotes"})
@Validated
public class QuoteController {

    private final CreateQuoteUseCase createQuoteUseCase;
    private final ManageQuoteUseCase manageQuoteUseCase;

    public QuoteController(CreateQuoteUseCase createQuoteUseCase, ManageQuoteUseCase manageQuoteUseCase) {
        this.createQuoteUseCase = createQuoteUseCase;
        this.manageQuoteUseCase = manageQuoteUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'WORKSHOP_ADMIN', 'ADMIN_TALLER', 'SERVICE_ADVISOR', 'ASESOR')")
    public ResponseEntity<QuoteResponse> createQuote(@Valid @RequestBody CreateQuoteRequest request) {
        CreateQuoteUseCase.CreateQuoteCommand command = new CreateQuoteUseCase.CreateQuoteCommand(
                request.workOrderId(),
                request.branchId(),
                request.discountPercentage(),
                request.notes()
        );
        Quote quote = createQuoteUseCase.createQuote(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(QuoteResponse.from(quote));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'WORKSHOP_ADMIN', 'ADMIN_TALLER', 'SERVICE_ADVISOR', 'ASESOR', 'TECHNICIAN', 'MECANICO')")
    public ResponseEntity<QuoteResponse> getQuote(@PathVariable UUID id) {
        Quote quote = manageQuoteUseCase.getQuote(id);
        return ResponseEntity.ok(QuoteResponse.from(quote));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'WORKSHOP_ADMIN', 'ADMIN_TALLER', 'SERVICE_ADVISOR', 'ASESOR', 'TECHNICIAN', 'MECANICO')")
    public ResponseEntity<List<QuoteResponse>> listQuotes(
            @RequestParam UUID branchId,
            @RequestParam(required = false) QuoteStatus status
    ) {
        List<Quote> quotes = manageQuoteUseCase.listQuotes(branchId, status);
        return ResponseEntity.ok(quotes.stream().map(QuoteResponse::from).toList());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'WORKSHOP_ADMIN', 'ADMIN_TALLER', 'SERVICE_ADVISOR', 'ASESOR')")
    public ResponseEntity<QuoteResponse> updateDiscount(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateQuoteDiscountRequest request
    ) {
        Quote updated = manageQuoteUseCase.updateDiscount(id, request.discountPercentage(), request.notes());
        return ResponseEntity.ok(QuoteResponse.from(updated));
    }

    @PostMapping("/{id}/approvals")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'WORKSHOP_ADMIN', 'ADMIN_TALLER', 'SERVICE_ADVISOR', 'ASESOR')")
    public ResponseEntity<QuoteResponse> approveQuote(@PathVariable UUID id) {
        Quote approved = manageQuoteUseCase.approveQuote(id);
        return ResponseEntity.ok(QuoteResponse.from(approved));
    }

    @PostMapping("/{id}/cancellations")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'WORKSHOP_ADMIN', 'ADMIN_TALLER', 'SERVICE_ADVISOR', 'ASESOR')")
    public ResponseEntity<QuoteResponse> cancelQuote(@PathVariable UUID id) {
        Quote canceled = manageQuoteUseCase.cancelQuote(id);
        return ResponseEntity.ok(QuoteResponse.from(canceled));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'WORKSHOP_ADMIN', 'ADMIN_TALLER', 'SERVICE_ADVISOR', 'ASESOR')")
    public ResponseEntity<QuoteResponse> deleteQuote(@PathVariable UUID id) {
        Quote canceled = manageQuoteUseCase.cancelQuote(id);
        return ResponseEntity.ok(QuoteResponse.from(canceled));
    }
}
