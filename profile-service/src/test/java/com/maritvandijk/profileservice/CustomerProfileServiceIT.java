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
    @DisplayName("returns 404 for an unknown customer")
    void returnsNotFoundForUnknownCustomer() {
        restTestClient.get()
                .uri("/profile/unknown")
                .exchange()
                .expectStatus().isNotFound();
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

        restTestClient.get()
                .uri("/profile/customer-1")
                .exchange()
                .expectStatus().isEqualTo(502);
    }

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

    // Documents current behavior: orderFuture.get() is awaited first, so when both
    // downstreams fail only OrderServiceException is reported and the recommendation
    // failure is silently dropped. A real implementation would aggregate or at least
    // log both failures.
    @Test
    @DisplayName("when both downstream services fail, only the first awaited failure is surfaced")
    void surfacesOnlyFirstFailureWhenBothDownstreamsFail() {
        stubFor(get(urlEqualTo("/orders/customer-1"))
                .willReturn(aResponse().withStatus(503)));
        stubFor(get(urlEqualTo("/recommendations/customer-1"))
                .willReturn(aResponse().withStatus(503)));

        restTestClient.get()
                .uri("/profile/customer-1")
                .exchange()
                .expectStatus().isEqualTo(502)
                .expectBody()
                .jsonPath("$.detail").isEqualTo("Order service unavailable");
    }

    // Documents current behavior: the per-future 2-second timeout fires, but the
    // resulting RuntimeException("Request timed out") isn't mapped by GlobalExceptionHandler,
    // so it surfaces as a generic 500. A real-world fix would map it to 504 Gateway Timeout.
    @Test
    @DisplayName("times out as 500 when the order service is slow")
    void timesOutWhenOrderServiceIsSlow() {
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
                        .withBody("""
                                [{"itemId":"rec-1","title":"Super Widget"}]
                                """)));

        restTestClient.get()
                .uri("/profile/customer-1")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    @DisplayName("times out as 500 when the recommendation service is slow")
    void timesOutWhenRecommendationServiceIsSlow() {
        stubFor(get(urlEqualTo("/orders/customer-1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
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
                .expectStatus().is5xxServerError();
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
