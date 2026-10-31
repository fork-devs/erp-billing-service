package com.erp.billingservice.infrastructure.client;

import com.erp.billingservice.domain.port.WorkOrderClientPort;
import com.erp.billingservice.infrastructure.client.workorder.WorkOrderClientAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WorkOrderClientAdapterTest {

    @Test
    @DisplayName("Should successfully fetch completed work order")
    void shouldFetchCompletedWorkOrder() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://work-order-service");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        WorkOrderClientAdapter adapter = new WorkOrderClientAdapter(builder, "http://work-order-service");

        UUID orderId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID partId = UUID.randomUUID();

        String json = """
                {
                    "orderId": "%s",
                    "number": "OT-001",
                    "status": "COMPLETED",
                    "branchId": "%s",
                    "customerId": "%s",
                    "items": [
                        {
                            "sparePartId": "%s",
                            "quantity": 2,
                            "laborPrice": 50.00
                        }
                    ]
                }
                """.formatted(orderId, branchId, customerId, partId);

        server.expect(requestTo("http://work-order-service/api/v1/work-orders/" + orderId))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        Optional<WorkOrderClientPort.WorkOrderDetail> result = adapter.getWorkOrder(orderId);

        assertThat(result).isPresent();
        assertThat(result.get().orderId()).isEqualTo(orderId);
        assertThat(result.get().status()).isEqualTo("COMPLETED");
        assertThat(result.get().items()).hasSize(1);
        server.verify();
    }

    @Test
    @DisplayName("Should handle 404 or error gracefully")
    void shouldHandleNotFoundGracefully() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://work-order-service");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        WorkOrderClientAdapter adapter = new WorkOrderClientAdapter(builder, "http://work-order-service");

        UUID orderId = UUID.randomUUID();
        server.expect(requestTo("http://work-order-service/api/v1/work-orders/" + orderId))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        Optional<WorkOrderClientPort.WorkOrderDetail> result = adapter.getWorkOrder(orderId);

        assertThat(result).isEmpty();
        server.verify();
    }
}
