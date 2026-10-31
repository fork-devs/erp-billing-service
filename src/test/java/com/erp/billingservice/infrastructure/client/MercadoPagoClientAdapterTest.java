package com.erp.billingservice.infrastructure.client;

import com.erp.billingservice.domain.port.MercadoPagoClientPort;
import com.erp.billingservice.infrastructure.client.mercadopago.MercadoPagoClientAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MercadoPagoClientAdapterTest {

    @Test
    @DisplayName("Should successfully verify approved payment")
    void shouldVerifyApprovedPayment() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.mercadopago.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        MercadoPagoClientAdapter adapter = new MercadoPagoClientAdapter(builder, "https://api.mercadopago.com", "test-token");

        String json = """
                {
                    "id": 98765432101,
                    "status": "approved",
                    "transaction_amount": 225.00,
                    "payment_method_id": "visa",
                    "currency_id": "PEN"
                }
                """;

        server.expect(requestTo("https://api.mercadopago.com/v1/payments/98765432101"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        Optional<MercadoPagoClientPort.PaymentVerification> result = adapter.verifyPayment("98765432101");

        assertThat(result).isPresent();
        assertThat(result.get().paymentId()).isEqualTo("98765432101");
        assertThat(result.get().status()).isEqualTo("approved");
        assertThat(result.get().transactionAmount()).isEqualByComparingTo(new BigDecimal("225.00"));
        server.verify();
    }

    @Test
    @DisplayName("Should handle 404 or network failure gracefully")
    void shouldHandleFailureGracefully() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.mercadopago.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        MercadoPagoClientAdapter adapter = new MercadoPagoClientAdapter(builder, "https://api.mercadopago.com", "test-token");

        server.expect(requestTo("https://api.mercadopago.com/v1/payments/99999"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        Optional<MercadoPagoClientPort.PaymentVerification> result = adapter.verifyPayment("99999");

        assertThat(result).isEmpty();
        server.verify();
    }
}
