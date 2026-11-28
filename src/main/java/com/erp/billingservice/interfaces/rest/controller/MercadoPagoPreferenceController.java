package com.erp.billingservice.interfaces.rest.controller;

import com.erp.billingservice.application.usecase.ManageQuoteUseCase;
import com.erp.billingservice.domain.exception.DomainValidationException;
import com.erp.billingservice.domain.model.Quote;
import com.erp.billingservice.domain.model.QuoteStatus;
import com.erp.billingservice.infrastructure.persistence.entity.PaymentIntentJpaEntity;
import com.erp.billingservice.infrastructure.persistence.repository.SpringDataPaymentIntentRepository;
import com.erp.billingservice.infrastructure.persistence.rls.TenantContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments/mercadopago")
public class MercadoPagoPreferenceController {

    private final ManageQuoteUseCase manageQuoteUseCase;
    private final SpringDataPaymentIntentRepository paymentIntentRepository;

    public MercadoPagoPreferenceController() {
        this(null, null);
    }

    @Autowired
    public MercadoPagoPreferenceController(
            @Autowired(required = false) ManageQuoteUseCase manageQuoteUseCase,
            @Autowired(required = false) SpringDataPaymentIntentRepository paymentIntentRepository) {
        this.manageQuoteUseCase = manageQuoteUseCase;
        this.paymentIntentRepository = paymentIntentRepository;
    }

    @PostMapping("/preferences")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'WORKSHOP_ADMIN', 'ADMIN_TALLER', 'SERVICE_ADVISOR', 'ASESOR', 'CUSTOMER', 'CLIENTE')")
    public ResponseEntity<Map<String, Object>> createPreference(@RequestBody(required = false) Map<String, Object> request) {
        String quoteIdStr = request != null && request.containsKey("quoteId")
                ? String.valueOf(request.get("quoteId"))
                : UUID.randomUUID().toString();
        UUID quoteId = UUID.fromString(quoteIdStr);
        BigDecimal amount = new BigDecimal("225.00");

        if (manageQuoteUseCase != null) {
            Quote quote = manageQuoteUseCase.getQuote(quoteId);
            if (quote.getStatus() != QuoteStatus.APPROVED) {
                throw new DomainValidationException("Quote must be APPROVED before creating Mercado Pago preference");
            }
            amount = quote.getTotalAmount();
        }

        String preferenceId = "pref-" + UUID.randomUUID();
        String externalRef = quoteId.toString();

        if (paymentIntentRepository != null && TenantContextHolder.getTenantId() != null) {
            Instant now = Instant.now();
            paymentIntentRepository.save(new PaymentIntentJpaEntity(
                    UUID.randomUUID(),
                    TenantContextHolder.getTenantId(),
                    quoteId,
                    preferenceId,
                    externalRef,
                    amount,
                    "PEN",
                    "PENDING",
                    request != null && request.get("type") != null ? String.valueOf(request.get("type")) : "RECEIPT",
                    request != null && request.get("customerDocumentType") != null ? String.valueOf(request.get("customerDocumentType")) : "DNI",
                    request != null && request.get("customerDocumentNumber") != null ? String.valueOf(request.get("customerDocumentNumber")) : "00000000",
                    request != null && request.get("customerName") != null ? String.valueOf(request.get("customerName")) : "Customer",
                    now,
                    now
            ));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "preferenceId", preferenceId,
                "initPoint", "https://www.mercadopago.com.pe/checkout/v1/redirect?pref_id=" + preferenceId,
                "sandboxInitPoint", "https://sandbox.mercadopago.com.pe/checkout/v1/redirect?pref_id=" + preferenceId,
                "amount", amount,
                "currency", "PEN",
                "externalReference", externalRef
        ));
    }
}
