package com.erp.billingservice.domain;

import com.erp.billingservice.domain.event.QuoteApprovedEvent;
import com.erp.billingservice.domain.event.QuoteCanceledEvent;
import com.erp.billingservice.domain.event.QuoteCreatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class QuoteEventTest {

    private final UUID tenantId = UUID.randomUUID();
    private final UUID quoteId = UUID.randomUUID();
    private final UUID workOrderId = UUID.randomUUID();
    private final UUID branchId = UUID.randomUUID();

    @Test
    @DisplayName("Should create QuoteCreatedEvent with all fields")
    void shouldCreateQuoteCreatedEvent() {
        QuoteCreatedEvent event = QuoteCreatedEvent.of(
                tenantId, quoteId, workOrderId, branchId,
                new BigDecimal("200.00"), new BigDecimal("10.00"), new BigDecimal("180.00")
        );

        assertThat(event.eventId()).isNotNull();
        assertThat(event.tenantId()).isEqualTo(tenantId);
        assertThat(event.quoteId()).isEqualTo(quoteId);
        assertThat(event.workOrderId()).isEqualTo(workOrderId);
        assertThat(event.branchId()).isEqualTo(branchId);
        assertThat(event.subtotalAmount()).isEqualTo(new BigDecimal("200.00"));
        assertThat(event.discountPercentage()).isEqualTo(new BigDecimal("10.00"));
        assertThat(event.totalAmount()).isEqualTo(new BigDecimal("180.00"));
        assertThat(event.occurredAt()).isNotNull();
    }

    @Test
    @DisplayName("Should create QuoteApprovedEvent with all fields")
    void shouldCreateQuoteApprovedEvent() {
        QuoteApprovedEvent event = QuoteApprovedEvent.of(tenantId, quoteId, workOrderId, branchId, new BigDecimal("180.00"));

        assertThat(event.eventId()).isNotNull();
        assertThat(event.tenantId()).isEqualTo(tenantId);
        assertThat(event.quoteId()).isEqualTo(quoteId);
        assertThat(event.workOrderId()).isEqualTo(workOrderId);
        assertThat(event.branchId()).isEqualTo(branchId);
        assertThat(event.totalAmount()).isEqualTo(new BigDecimal("180.00"));
        assertThat(event.occurredAt()).isNotNull();
    }

    @Test
    @DisplayName("Should create QuoteCanceledEvent with all fields")
    void shouldCreateQuoteCanceledEvent() {
        QuoteCanceledEvent event = QuoteCanceledEvent.of(tenantId, quoteId, workOrderId, branchId);

        assertThat(event.eventId()).isNotNull();
        assertThat(event.tenantId()).isEqualTo(tenantId);
        assertThat(event.quoteId()).isEqualTo(quoteId);
        assertThat(event.workOrderId()).isEqualTo(workOrderId);
        assertThat(event.branchId()).isEqualTo(branchId);
        assertThat(event.occurredAt()).isNotNull();
    }
}
