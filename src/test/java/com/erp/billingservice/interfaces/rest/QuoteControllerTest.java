package com.erp.billingservice.interfaces.rest;

import com.erp.billingservice.application.usecase.CreateQuoteUseCase;
import com.erp.billingservice.application.usecase.ManageQuoteUseCase;
import com.erp.billingservice.domain.model.Quote;
import com.erp.billingservice.domain.model.QuoteItem;
import com.erp.billingservice.domain.model.QuoteItemType;
import com.erp.billingservice.domain.model.QuoteStatus;
import com.erp.billingservice.interfaces.rest.advice.GlobalRestExceptionHandler;
import com.erp.billingservice.interfaces.rest.controller.QuoteController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class QuoteControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CreateQuoteUseCase createQuoteUseCase;

    @Mock
    private ManageQuoteUseCase manageQuoteUseCase;

    @InjectMocks
    private QuoteController quoteController;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID quoteId = UUID.randomUUID();
    private final UUID workOrderId = UUID.randomUUID();
    private final UUID branchId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(quoteController)
                .setControllerAdvice(new GlobalRestExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/billing/quotes creates a quote and returns 201 Created")
    void shouldCreateQuote() throws Exception {
        QuoteItem item = new QuoteItem(UUID.randomUUID(), tenantId, null, QuoteItemType.SERVICE,
                "Brake test", BigDecimal.ONE, new BigDecimal("100.00"), null);
        Quote quote = Quote.create(tenantId, workOrderId, branchId, new BigDecimal("10.00"), "Notes", List.of(item));

        when(createQuoteUseCase.createQuote(any(CreateQuoteUseCase.CreateQuoteCommand.class))).thenReturn(quote);

        String json = """
                {
                    "workOrderId": "%s",
                    "branchId": "%s",
                    "discountPercentage": 10.0,
                    "notes": "Notes"
                }
                """.formatted(workOrderId, branchId);

        mockMvc.perform(post("/api/v1/billing/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.workOrderId").value(workOrderId.toString()))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.subtotalAmount").value(100.0))
                .andExpect(jsonPath("$.totalAmount").value(90.0));
    }

    @Test
    @DisplayName("GET /api/v1/billing/quotes/{id} returns quote detail")
    void shouldGetQuoteDetail() throws Exception {
        QuoteItem item = new QuoteItem(UUID.randomUUID(), tenantId, null, QuoteItemType.SERVICE,
                "Oil change", BigDecimal.ONE, new BigDecimal("80.00"), null);
        Quote quote = Quote.create(tenantId, workOrderId, branchId, BigDecimal.ZERO, null, List.of(item));

        when(manageQuoteUseCase.getQuote(quoteId)).thenReturn(quote);

        mockMvc.perform(get("/api/v1/billing/quotes/" + quoteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workOrderId").value(workOrderId.toString()))
                .andExpect(jsonPath("$.items[0].name").value("Oil change"));
    }

    @Test
    @DisplayName("GET /api/v1/billing/quotes?branchId={branchId} lists quotes for a branch")
    void shouldListQuotesByBranch() throws Exception {
        Quote quote = Quote.create(tenantId, workOrderId, branchId, BigDecimal.ZERO, null, List.of());
        when(manageQuoteUseCase.listQuotes(eq(branchId), any())).thenReturn(List.of(quote));

        mockMvc.perform(get("/api/v1/billing/quotes")
                        .param("branchId", branchId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].branchId").value(branchId.toString()));
    }

    @Test
    @DisplayName("PUT /api/v1/billing/quotes/{id} updates discount and recalculates totals")
    void shouldUpdateDiscount() throws Exception {
        QuoteItem item = new QuoteItem(UUID.randomUUID(), tenantId, null, QuoteItemType.SERVICE,
                "Repair", BigDecimal.ONE, new BigDecimal("100.00"), null);
        Quote quote = Quote.create(tenantId, workOrderId, branchId, BigDecimal.ZERO, null, List.of(item));
        quote.updateDiscount(new BigDecimal("15.00"), "Discount note");

        when(manageQuoteUseCase.updateDiscount(eq(quoteId), any(BigDecimal.class), eq("Discount note")))
                .thenReturn(quote);

        String json = """
                {
                    "discountPercentage": 15.0,
                    "notes": "Discount note"
                }
                """;

        mockMvc.perform(put("/api/v1/billing/quotes/" + quoteId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discountPercentage").value(15.0))
                .andExpect(jsonPath("$.totalAmount").value(85.0));
    }

    @Test
    @DisplayName("POST /api/v1/billing/quotes/{id}/approvals approves quote")
    void shouldApproveQuote() throws Exception {
        Quote quote = Quote.create(tenantId, workOrderId, branchId, BigDecimal.ZERO, null, List.of());
        quote.approve();

        when(manageQuoteUseCase.approveQuote(quoteId)).thenReturn(quote);

        mockMvc.perform(post("/api/v1/billing/quotes/" + quoteId + "/approvals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("POST /api/v1/billing/quotes/{id}/cancellations cancels quote")
    void shouldCancelQuote() throws Exception {
        Quote quote = Quote.create(tenantId, workOrderId, branchId, BigDecimal.ZERO, null, List.of());
        quote.cancel();

        when(manageQuoteUseCase.cancelQuote(quoteId)).thenReturn(quote);

        mockMvc.perform(post("/api/v1/billing/quotes/" + quoteId + "/cancellations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));
    }
}
