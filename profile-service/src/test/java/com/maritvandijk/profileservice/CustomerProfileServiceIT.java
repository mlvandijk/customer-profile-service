package com.maritvandijk.profileservice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureRestTestClient
@EnableWireMock(@ConfigureWireMock(port = 0, portProperties = "wiremock.server.port"))
// @ActiveProfiles("test") activates application-test.properties, which overrides
// services.base-url to point at the WireMock server instead of localhost:8081.
class CustomerProfileServiceIT {

    @Autowired
    private RestTestClient restTestClient;

    @Test
    @DisplayName("returns customer profile for a known customer")
    void returnsCustomerProfileForKnownCustomer() {
        stubFor(get(urlEqualTo("/orders/customer-1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [{"orderId":"order-1","description":"Widget"},{"orderId":"order-2","description":"Gadget"}]
                                """)));
        stubFor(get(urlEqualTo("/recommendations/customer-1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [{"itemId":"rec-1","title":"Super Widget"},{"itemId":"rec-2","title":"Mega Gadget"}]
                                """)));

        restTestClient.get()
                .uri("/profile/customer-1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.customerId").isEqualTo("customer-1")
                .jsonPath("$.orders[0].orderId").isEqualTo("order-1")
                .jsonPath("$.orders[1].orderId").isEqualTo("order-2")
                .jsonPath("$.recommendations[0].itemId").isEqualTo("rec-1")
                .jsonPath("$.recommendations[1].itemId").isEqualTo("rec-2");
    }

    @Test
    @DisplayName("returns 502 when the order service fails")
    void returnsBadGatewayWhenOrderServiceFails() {
        stubFor(get(urlEqualTo("/orders/customer-1"))
                .willReturn(aResponse().withStatus(503)));
        stubFor(get(urlEqualTo("/recommendations/customer-1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [{"itemId":"rec-1","title":"Super Widget"}]
                                """)));

        // On the modern branch, StructuredTaskScope cancels the sibling subtask automatically
        // and rethrows the failing subtask's exception directly from scope.join().
        restTestClient.get()
                .uri("/profile/customer-1")
                .exchange()
                .expectStatus().isEqualTo(502);
    }

    // On the modern branch, StructuredTaskScope surfaces failure consistently —
    // no silent swallowing of the second exception.
    @Test
    @DisplayName("returns 502 when the recommendation service fails")
    void returnsBadGatewayWhenRecommendationServiceFails() {
        stubFor(get(urlEqualTo("/orders/customer-1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [{"orderId":"order-1","description":"Widget"}]
                                """)));
        stubFor(get(urlEqualTo("/recommendations/customer-1"))
                .willReturn(aResponse().withStatus(503)));

        restTestClient.get()
                .uri("/profile/customer-1")
                .exchange()
                .expectStatus().isEqualTo(502);
    }

    @Test
    @DisplayName("returns 5xx when both services fail")
    void returns5xxWhenBothServicesFail() {
        stubFor(get(urlEqualTo("/orders/customer-1"))
                .willReturn(aResponse().withStatus(503)));
        stubFor(get(urlEqualTo("/recommendations/customer-1"))
                .willReturn(aResponse().withStatus(503)));

        restTestClient.get()
                .uri("/profile/customer-1")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    @DisplayName("returns 504 when request times out")
    void returnsGatewayTimeoutWhenRequestTimesOut() {
        stubFor(get(urlEqualTo("/orders/customer-1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withFixedDelay(3000)
                        .withBody("""
                                [{"orderId":"order-1","description":"Widget"}]
                                """)));
        stubFor(get(urlEqualTo("/recommendations/customer-1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withFixedDelay(3000)
                        .withBody("""
                                [{"itemId":"rec-1","title":"Super Widget"}]
                                """)));

        restTestClient.get()
                .uri("/profile/customer-1")
                .exchange()
                .expectStatus().isEqualTo(504);
    }

    // Regression guard for the parallelism claim: with each downstream stubbed at
    // 500ms, the request must complete well under the 1000ms sequential floor.
    // If a future refactor accidentally serializes the calls, this assertion fires.
    @Test
    @DisplayName("calls downstream services in parallel")
    void callsDownstreamsInParallel() {
        stubFor(get(urlEqualTo("/orders/customer-1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withFixedDelay(500)
                        .withBody("""
                                [{"orderId":"order-1","description":"Widget"}]
                                """)));
        stubFor(get(urlEqualTo("/recommendations/customer-1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withFixedDelay(500)
                        .withBody("""
                                [{"itemId":"rec-1","title":"Super Widget"}]
                                """)));

        long startNanos = System.nanoTime();
        restTestClient.get()
                .uri("/profile/customer-1")
                .exchange()
                .expectStatus().isOk();
        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;

        assertThat(elapsedMs).isLessThan(900);
    }
}
