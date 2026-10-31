package com.erp.billingservice.domain;

import com.erp.billingservice.domain.exception.DomainValidationException;
import com.erp.billingservice.domain.model.Quote;
import com.erp.billingservice.domain.model.QuoteItem;
import com.erp.billingservice.domain.model.QuoteItemType;
import com.erp.billingservice.domain.model.QuoteStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuoteDomainTest {

    private final UUID tenantId = UUID.randomUUID();
    private final UUID workOrderId = UUID.randomUUID();
    private final UUID branchId = UUID.randomUUID();

    @Test
    @DisplayName("Should create quote and calculate subtotal and total after discount correctly")
    void shouldCreateQuoteWithTotals() {
        QuoteItem serviceItem = new QuoteItem(
                UUID.randomUUID(), tenantId, null, QuoteItemType.SERVICE,
                "Oil change", new BigDecimal("1.00"), new BigDecimal("80.00"), null
        );
        QuoteItem productItem = new QuoteItem(
                UUID.randomUUID(), tenantId, null, QuoteItemType.PRODUCT,
                "Brake pads", new BigDecimal("2.00"), new BigDecimal("85.00"), null
        );

        Quote quote = Quote.create(
                tenantId, workOrderId, branchId,
                new BigDecimal("10.00"), "Initial quote", List.of(serviceItem, productItem)
        );

        assertThat(quote.getId()).isNotNull();
        assertThat(quote.getTenantId()).isEqualTo(tenantId);
        assertThat(quote.getWorkOrderId()).isEqualTo(workOrderId);
        assertThat(quote.getBranchId()).isEqualTo(branchId);
        assertThat(quote.getStatus()).isEqualTo(QuoteStatus.DRAFT);
        // Subtotal: 80.00 + (2 * 85.00) = 250.00
        assertThat(quote.getSubtotalAmount()).isEqualByComparingTo(new BigDecimal("250.00"));
        assertThat(quote.getDiscountPercentage()).isEqualByComparingTo(new BigDecimal("10.00"));
        // Total: 250 * 0.90 = 225.00
        assertThat(quote.getTotalAmount()).isEqualByComparingTo(new BigDecimal("225.00"));
    }

    @Test
    @DisplayName("Should update discount and recalculate total on DRAFT quote")
    void shouldUpdateDiscountOnDraftQuote() {
        QuoteItem item = new QuoteItem(
                UUID.randomUUID(), tenantId, null, QuoteItemType.SERVICE,
                "Inspection", new BigDecimal("1.00"), new BigDecimal("100.00"), null
        );
        Quote quote = Quote.create(tenantId, workOrderId, branchId, BigDecimal.ZERO, null, List.of(item));

        assertThat(quote.getTotalAmount()).isEqualByComparingTo(new BigDecimal("100.00"));

        quote.updateDiscount(new BigDecimal("15.00"), "Special promo");

        assertThat(quote.getDiscountPercentage()).isEqualByComparingTo(new BigDecimal("15.00"));
        assertThat(quote.getTotalAmount()).isEqualByComparingTo(new BigDecimal("85.00"));
        assertThat(quote.getNotes()).isEqualTo("Special promo");
    }

    @Test
    @DisplayName("Should approve DRAFT quote and prevent subsequent discount updates")
    void shouldApproveDraftQuote() {
        Quote quote = Quote.create(tenantId, workOrderId, branchId, BigDecimal.ZERO, null, List.of());
        quote.approve();

        assertThat(quote.getStatus()).isEqualTo(QuoteStatus.APPROVED);
        assertThat(quote.getApprovedAt()).isNotNull();

        assertThatThrownBy(() -> quote.updateDiscount(new BigDecimal("5.00"), null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Cannot update discount on quote with status: APPROVED");

        assertThatThrownBy(quote::approve)
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Only DRAFT quotes can be approved");
    }

    @Test
    @DisplayName("Should cancel quote and reject modifications on canceled quote")
    void shouldCancelQuote() {
        Quote quote = Quote.create(tenantId, workOrderId, branchId, BigDecimal.ZERO, null, List.of());
        quote.cancel();

        assertThat(quote.getStatus()).isEqualTo(QuoteStatus.CANCELED);
        assertThat(quote.getCanceledAt()).isNotNull();

        assertThatThrownBy(quote::cancel)
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Quote is already canceled");
    }

    @Test
    @DisplayName("Should validate invalid parameters for quote creation and item creation")
    void shouldValidateInvalidParameters() {
        assertThatThrownBy(() -> Quote.create(null, workOrderId, branchId, BigDecimal.ZERO, null, List.of()))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Tenant ID is required");

        assertThatThrownBy(() -> Quote.create(tenantId, null, branchId, BigDecimal.ZERO, null, List.of()))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Work order ID is required");

        assertThatThrownBy(() -> Quote.create(tenantId, workOrderId, branchId, new BigDecimal("110.00"), null, List.of()))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Discount percentage must be between 0 and 100");

        assertThatThrownBy(() -> new QuoteItem(UUID.randomUUID(), tenantId, null, QuoteItemType.SERVICE,
                "", new BigDecimal("1"), new BigDecimal("10"), null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("name is required");

        assertThatThrownBy(() -> new QuoteItem(UUID.randomUUID(), tenantId, null, QuoteItemType.SERVICE,
                "Task", new BigDecimal("0"), new BigDecimal("10"), null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Quantity must be greater than zero");

        assertThatThrownBy(() -> new QuoteItem(UUID.randomUUID(), tenantId, null, QuoteItemType.SERVICE,
                "Task", new BigDecimal("1"), new BigDecimal("-1"), null))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("Unit price cannot be negative");
    }
}
