package com.erp.billingservice.infrastructure;

import com.erp.billingservice.domain.model.Correlative;
import com.erp.billingservice.domain.model.Currency;
import com.erp.billingservice.domain.model.FiscalEmissionResult;
import com.erp.billingservice.domain.model.Invoice;
import com.erp.billingservice.domain.model.InvoiceLine;
import com.erp.billingservice.domain.model.InvoiceStatus;
import com.erp.billingservice.domain.model.Series;
import com.erp.billingservice.domain.model.VoucherType;
import com.erp.billingservice.infrastructure.client.factos.FactosClientAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class FactosClientAdapterTest {

    private final UUID tenantId = UUID.randomUUID();
    private final UUID workOrderId = UUID.randomUUID();

    @Test
    @DisplayName("Should successfully emit invoice to factos and receive ACCEPTED status with pdfUrl")
    void shouldSuccessfullyEmitInvoice() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8080");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        FactosClientAdapter adapter = new FactosClientAdapter(builder, "http://localhost:8080", "test-api-key");

        String mockResponseBody = """
            {
              "series": "F001",
              "correlative": "00000001",
              "status": "EMITTED",
              "totalTaxable": 5000.00,
              "totalIgv": 900.00,
              "totalAmount": 5900.00,
              "pdfUrl": "/api/v1/rendering/pdf/F001/00000001"
            }
            """;

        server.expect(requestTo("http://localhost:8080/api/v1/documents"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-API-KEY", "test-api-key"))
                .andRespond(withSuccess(mockResponseBody, MediaType.APPLICATION_JSON));

        InvoiceLine line = new InvoiceLine(
                UUID.randomUUID(), tenantId, "SERV-01", "Maintenance",
                new BigDecimal("1"), new BigDecimal("5900.00"), "TAXABLE_ONEROUS"
        );
        Invoice invoice = new Invoice(
                UUID.randomUUID(), tenantId, workOrderId, VoucherType.FACTURA,
                new Series("F001"), new Correlative(1), "20123456789",
                "CLIENT SAC", Currency.PEN, List.of(line),
                InvoiceStatus.DRAFT, null, null, null, null, null
        );

        FiscalEmissionResult result = adapter.emitDocument(invoice, "20123456789");

        server.verify();
        assertNotNull(result);
        assertEquals("ACCEPTED", result.status());
        assertEquals("/api/v1/rendering/pdf/F001/00000001", result.pdfUrl());
    }

    @Test
    @DisplayName("Should trigger fallback and return PENDING_SUNAT when factos API fails")
    void shouldFallbackWhenFactosFails() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8080");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        FactosClientAdapter adapter = new FactosClientAdapter(builder, "http://localhost:8080", "test-api-key");

        server.expect(requestTo("http://localhost:8080/api/v1/documents"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        InvoiceLine line = new InvoiceLine(
                UUID.randomUUID(), tenantId, "SERV-01", "Maintenance",
                new BigDecimal("1"), new BigDecimal("5900.00"), "TAXABLE_ONEROUS"
        );
        Invoice invoice = new Invoice(
                UUID.randomUUID(), tenantId, workOrderId, VoucherType.FACTURA,
                new Series("F001"), new Correlative(1), "20123456789",
                "CLIENT SAC", Currency.PEN, List.of(line),
                InvoiceStatus.DRAFT, null, null, null, null, null
        );

        FiscalEmissionResult result = adapter.emitDocumentFallback(invoice, "20123456789", new RuntimeException("500 Server Error"));

        assertNotNull(result);
        assertEquals("PENDING_SUNAT", result.status());
        assertNull(result.pdfUrl());
    }

    @Test
    @DisplayName("Should download PDF binary successfully from factos")
    void shouldDownloadPdfSuccessfully() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:8080");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        FactosClientAdapter adapter = new FactosClientAdapter(builder, "http://localhost:8080", "test-api-key");

        byte[] fakePdfBytes = "%PDF-1.4 mock pdf content".getBytes();

        server.expect(requestTo("http://localhost:8080/api/v1/rendering/pdf/F001/00000001"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-API-KEY", "test-api-key"))
                .andRespond(withSuccess(fakePdfBytes, MediaType.APPLICATION_PDF));

        byte[] downloaded = adapter.downloadPdf("F001", "00000001");

        server.verify();
        assertArrayEquals(fakePdfBytes, downloaded);
    }
}
