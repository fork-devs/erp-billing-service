package com.erp.billingservice.infrastructure.client.workorder;

import com.erp.billingservice.domain.port.WorkOrderClientPort;
import com.erp.billingservice.infrastructure.persistence.rls.TenantContextHolder;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class WorkOrderClientAdapter implements WorkOrderClientPort {

    private static final Logger log = LoggerFactory.getLogger(WorkOrderClientAdapter.class);

    private final RestClient restClient;

    public WorkOrderClientAdapter(
            RestClient.Builder restClientBuilder,
            @Value("${erp.clients.work-order.url:http://erp-work-order-service:8088}") String baseUrl
    ) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .build();
    }

    public WorkOrderClientAdapter(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    @CircuitBreaker(name = "workOrderClient", fallbackMethod = "getWorkOrderFallback")
    @Retry(name = "workOrderClient")
    public Optional<WorkOrderDetail> getWorkOrder(UUID workOrderId) {
        UUID tenantId = TenantContextHolder.getTenantId();
        try {
            WorkOrderApiResponse response = restClient.get()
                    .uri("/api/v1/work-orders/{id}", workOrderId)
                    .header("X-Tenant-ID", tenantId != null ? tenantId.toString() : "")
                    .retrieve()
                    .body(WorkOrderApiResponse.class);

            if (response == null) {
                return Optional.empty();
            }

            List<WorkOrderItem> items = new ArrayList<>();
            if (response.items() != null) {
                for (WorkOrderItemApiResponse item : response.items()) {
                    items.add(new WorkOrderItem(
                            "PRODUCT",
                            "Spare part " + (item.sparePartId() != null ? item.sparePartId() : ""),
                            item.quantity() != null ? new BigDecimal(item.quantity()) : BigDecimal.ONE,
                            item.laborPrice() != null ? item.laborPrice() : BigDecimal.ZERO,
                            (item.laborPrice() != null && item.quantity() != null)
                                    ? item.laborPrice().multiply(new BigDecimal(item.quantity()))
                                    : BigDecimal.ZERO
                    ));
                }
            }

            return Optional.of(new WorkOrderDetail(
                    response.orderId(),
                    response.number(),
                    response.status(),
                    response.branchId(),
                    response.customerId(),
                    "Customer " + response.customerId(),
                    "00000000",
                    items
            ));
        } catch (Exception e) {
            log.warn("Error calling work order service for order {}: {}", workOrderId, e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<WorkOrderDetail> getWorkOrderFallback(UUID workOrderId, Throwable t) {
        log.warn("Fallback triggered for getWorkOrder {}: {}", workOrderId, t.getMessage());
        return Optional.empty();
    }

    public record WorkOrderApiResponse(
            UUID orderId,
            String number,
            String status,
            UUID branchId,
            UUID customerId,
            List<WorkOrderItemApiResponse> items
    ) {}

    public record WorkOrderItemApiResponse(
            UUID sparePartId,
            Integer quantity,
            BigDecimal laborPrice
    ) {}
}
