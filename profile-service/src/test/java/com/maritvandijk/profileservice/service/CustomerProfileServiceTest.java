package com.maritvandijk.profileservice.service;

import com.maritvandijk.profileservice.client.OrderServiceClient;
import com.maritvandijk.profileservice.client.RecommendationServiceClient;
import com.maritvandijk.profileservice.exception.OrderServiceException;
import com.maritvandijk.profileservice.exception.RecommendationServiceException;
import com.maritvandijk.profileservice.model.CustomerProfile;
import com.maritvandijk.profileservice.model.Order;
import com.maritvandijk.profileservice.model.Recommendation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerProfileServiceTest {

    @Mock
    private OrderServiceClient orderClient;

    @Mock
    private RecommendationServiceClient recommendationClient;

    @Test
    @DisplayName("returns profile for a known customer")
    void returnsProfileForKnownCustomer() throws Exception {
        CustomerProfileService service = new CustomerProfileService(orderClient, recommendationClient);
        when(orderClient.getOrders(any())).thenReturn(List.of(new Order("order-1", "Widget")));
        when(recommendationClient.getRecommendations(any()))
                .thenReturn(List.of(new Recommendation("rec-1", "Super Widget")));

        CustomerProfile profile = service.getProfile("customer-1");

        assertThat(profile.customerId()).isEqualTo("customer-1");
        assertThat(profile.orders().getFirst().orderId()).isEqualTo("order-1");
        assertThat(profile.recommendations().getFirst().itemId()).isEqualTo("rec-1");
    }

    @Test
    @DisplayName("throws OrderServiceException when order client throws")
    void throwsOrderServiceExceptionWhenOrderClientThrows() {
        CustomerProfileService service = new CustomerProfileService(orderClient, recommendationClient);
        when(orderClient.getOrders(any())).thenThrow(serviceUnavailable());
        when(recommendationClient.getRecommendations(any()))
                .thenReturn(List.of(new Recommendation("rec-1", "Super Widget")));

        assertThatThrownBy(() -> service.getProfile("customer-1"))
                .isInstanceOf(OrderServiceException.class);
    }

    @Test
    @DisplayName("throws RecommendationServiceException when recommendation client throws")
    void throwsRecommendationServiceExceptionWhenRecommendationClientThrows() {
        CustomerProfileService service = new CustomerProfileService(orderClient, recommendationClient);
        when(orderClient.getOrders(any())).thenReturn(List.of(new Order("order-1", "Widget")));
        when(recommendationClient.getRecommendations(any())).thenThrow(serviceUnavailable());

        assertThatThrownBy(() -> service.getProfile("customer-1"))
                .isInstanceOf(RecommendationServiceException.class);
    }

    @Test
    @DisplayName("cancels the other future when one task times out")
    void cancelsTheOtherFutureWhenOneTaskTimesOut() {
        CustomerProfileService service = new CustomerProfileService(orderClient, recommendationClient);
        CountDownLatch releaseLatch = new CountDownLatch(1);
        when(orderClient.getOrders(any())).thenAnswer(_ -> {
            releaseLatch.await(3, TimeUnit.SECONDS);
            return List.of(new Order("order-1", "Widget"));
        });
        when(recommendationClient.getRecommendations(any()))
                .thenReturn(List.of(new Recommendation("rec-1", "Super Widget")));

        assertThatThrownBy(() -> service.getProfile("customer-1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("timed out");

        assertThat(releaseLatch.getCount()).isEqualTo(1);
        releaseLatch.countDown();
    }

    private RestClientResponseException serviceUnavailable() {
        return new RestClientResponseException(
                "Service unavailable",
                HttpStatus.SERVICE_UNAVAILABLE,
                "Service Unavailable",
                HttpHeaders.EMPTY,
                new byte[0],
                StandardCharsets.UTF_8);
    }
}
