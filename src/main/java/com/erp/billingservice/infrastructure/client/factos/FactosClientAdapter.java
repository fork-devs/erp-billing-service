package com.erp.billingservice.infrastructure.client.factos;

import com.erp.billingservice.domain.model.FiscalEmissionResult;
import com.erp.billingservice.domain.model.Invoice;
import com.erp.billingservice.domain.model.InvoiceLine;
import com.erp.billingservice.domain.port.FiscalProviderPort;
import com.erp.billingservice.infrastructure.client.factos.dto.FactosDocumentRequest;
import com.erp.billingservice.infrastructure.client.factos.dto.FactosDocumentResponse;
import com.erp.billingservice.infrastructure.client.factos.dto.FactosItemDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Component
public class FactosClientAdapter implements FiscalProviderPort {

    private static final Logger log = LoggerFactory.getLogger(FactosClientAdapter.class);

    private final RestClient restClient;
    private final String apiKey;

    @org.springframework.beans.factory.annotation.Autowired
    public FactosClientAdapter(
            RestClient.Builder restClientBuilder,
            @Value("${erp.factos.base-url:https://factos-reva.onrender.com}") String baseUrl,
            @Value("${erp.factos.api-key:mock-key}") String apiKey
    ) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .build();
        this.apiKey = apiKey;
    }

    public FactosClientAdapter(RestClient restClient, String apiKey) {
        this.restClient = restClient;
        this.apiKey = apiKey;
    }

    @Override
    @CircuitBreaker(name = "factos", fallbackMethod = "emitDocumentFallback")
    @Retry(name = "factos")
    public FiscalEmissionResult emitDocument(Invoice invoice, String issuerRuc) {
        List<FactosItemDto> itemDtos = invoice.getLines().stream()
                .map(this::toItemDto)
                .toList();

        LocalDate issueDate = LocalDate.ofInstant(invoice.getIssuedAt(), ZoneOffset.UTC);

        FactosDocumentRequest request = new FactosDocumentRequest(
                invoice.getSeries().getValue(),
                invoice.getCorrelative().toFormattedString(),
                invoice.getVoucherType().getCpeCode(),
                issueDate.toString(),
                issuerRuc != null ? issuerRuc : "20123456789",
                invoice.getCustomerDocument(),
                invoice.getCustomerName(),
                invoice.getCurrency().name(),
                itemDtos
        );

        log.info("Sending invoice {} to factos API", invoice.getSeries().getValue() + "-" + invoice.getCorrelative());

        FactosDocumentResponse response = restClient.post()
                .uri("/api/v1/documents")
                .header("X-API-KEY", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(FactosDocumentResponse.class);

        if (response != null && "EMITTED".equalsIgnoreCase(response.status())) {
            return new FiscalEmissionResult("ACCEPTED", response.pdfUrl(), null);
        }

        return new FiscalEmissionResult(
                response != null ? response.status() : "PENDING_SUNAT",
                response != null ? response.pdfUrl() : null,
                null
        );
    }

    public FiscalEmissionResult emitDocumentFallback(Invoice invoice, String issuerRuc, Throwable throwable) {
        log.warn("Factos service unavailable or failed for invoice {}-{}. Fallback to PENDING_SUNAT. Reason: {}",
                invoice.getSeries(), invoice.getCorrelative(), throwable.getMessage());
        return new FiscalEmissionResult("PENDING_SUNAT", null, null);
    }

    @Override
    @CircuitBreaker(name = "factos", fallbackMethod = "downloadPdfFallback")
    @Retry(name = "factos")
    public byte[] downloadPdf(String series, String correlative) {
        return restClient.get()
                .uri("/api/v1/rendering/pdf/{series}/{correlative}", series, correlative)
                .header("X-API-KEY", apiKey)
                .accept(MediaType.APPLICATION_PDF)
                .retrieve()
                .body(byte[].class);
    }

    public byte[] downloadPdfFallback(String series, String correlative, Throwable throwable) {
        log.warn("Failed to download PDF from factos for {}-{}: {}", series, correlative, throwable.getMessage());
        return new byte[0];
    }

    @Override
    public byte[] generateQr(String content) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/rendering/qr")
                        .queryParam("content", content)
                        .build())
                .header("X-API-KEY", apiKey)
                .accept(MediaType.IMAGE_PNG)
                .retrieve()
                .body(byte[].class);
    }

    private FactosItemDto toItemDto(InvoiceLine line) {
        return new FactosItemDto(
                line.getCode(),
                line.getDescription(),
                line.getQuantity(),
                line.getUnitPrice(),
                line.getAffectationType()
        );
    }
}
