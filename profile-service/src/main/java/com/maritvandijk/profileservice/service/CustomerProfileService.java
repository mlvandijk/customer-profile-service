package com.maritvandijk.profileservice.service;

import com.maritvandijk.profileservice.client.OrderServiceClient;
import com.maritvandijk.profileservice.client.RecommendationServiceClient;
import com.maritvandijk.profileservice.exception.OrderServiceException;
import com.maritvandijk.profileservice.model.CustomerProfile;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Joiner;
import java.util.concurrent.TimeoutException;

@Service
public class CustomerProfileService {

    private static final ScopedValue<String> CUSTOMER_ID = ScopedValue.newInstance();

    private final OrderServiceClient orderServiceClient;
    private final RecommendationServiceClient recommendationServiceClient;

    public CustomerProfileService(OrderServiceClient orderServiceClient,
                                  RecommendationServiceClient recommendationServiceClient) {
        this.orderServiceClient = orderServiceClient;
        this.recommendationServiceClient = recommendationServiceClient;
    }

    public CustomerProfile getProfile(String customerId) throws InterruptedException, TimeoutException {
        try {
            return ScopedValue.where(CUSTOMER_ID, customerId).call(() -> {
                try (var scope = StructuredTaskScope.open(
                        Joiner.awaitAllSuccessfulOrThrow(),
                        config -> config.withName("customer-profile").withTimeout(Duration.ofSeconds(2)))) {
                    var orderTask = scope.fork(() -> orderServiceClient.getOrders(CUSTOMER_ID.get()));
                    var recTask = scope.fork(() -> recommendationServiceClient.getRecommendations(CUSTOMER_ID.get()));
                    scope.join();
                    return new CustomerProfile(customerId, orderTask.get(), recTask.get());
                } catch (ExecutionException e) {
                    switch (e.getCause()) {
                        case StructuredTaskScope.CancelledByTimeoutException _ -> throw new TimeoutException("Request timed out");
                        case OrderServiceException ose -> throw ose;
                        case RuntimeException rte -> throw rte;
                        default -> throw new RuntimeException(e.getCause());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted", e);
                }
            });
        } catch (InterruptedException | TimeoutException | RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}