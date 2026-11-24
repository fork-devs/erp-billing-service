package com.erp.billingservice.interfaces.rest.controller;

import com.erp.billingservice.domain.exception.DomainValidationException;
import com.erp.billingservice.domain.model.Quote;
import com.erp.billingservice.domain.model.QuoteItem;
import com.erp.billingservice.domain.model.QuoteItemType;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuoteSyncAndSessionTest {

    @Test
    void draftQuoteRecalculatesTotalsOnSyncItems() {
        UUID tenantId = UUID.randomUUID();
        Quote quote = Quote.create(tenantId, UUID.randomUUID(), UUID.randomUUID(), BigDecimal.ZERO, "Init", List.of());
        QuoteItem item = new QuoteItem(
                UUID.randomUUID(), tenantId, quote.getId(), QuoteItemType.SERVICE,
                "Alignment", BigDecimal.ONE, new BigDecimal("120.00"), new BigDecimal("120.00")
        );
        quote.syncItems(List.of(item));
        assertEquals(new BigDecimal("120.00"), quote.getTotalAmount());

        quote.approve();
        assertThrows(DomainValidationException.class, () -> quote.syncItems(List.of()));
    }

    @Test
    void paymentSessionControllerCreatesMercadoPagoSession() {
        PaymentSessionController controller = new PaymentSessionController();
        UUID quoteId = UUID.randomUUID();
        ResponseEntity<PaymentSessionController.PaymentSessionResponse> res = controller.createPaymentSession(
                new PaymentSessionController.CreatePaymentSessionRequest(quoteId, null, "MERCADOPAGO")
        );
        assertEquals(201, res.getStatusCode().value());
        assertEquals("MERCADOPAGO", res.getBody().provider());
        assertEquals("PENDING", res.getBody().status());
        assertNotNull(res.getBody().checkoutUrl());
    }
}
