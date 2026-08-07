package com.maritvandijk.profileservice.service;

import com.maritvandijk.profileservice.client.OrderServiceClient;
import com.maritvandijk.profileservice.client.RecommendationServiceClient;
import com.maritvandijk.profileservice.exception.OrderServiceException;
import com.maritvandijk.profileservice.exception.RecommendationServiceException;
import com.maritvandijk.profileservice.model.CustomerProfile;
import com.maritvandijk.profileservice.model.Order;
import com.maritvandijk.profileservice.model.Recommendation;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.concurrent.*;

@Service
public class CustomerProfileService {

    private static final ScopedValue<String> CUSTOMER_ID = ScopedValue.newInstance();

    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    private final OrderServiceClient orderServiceClient;
    private final RecommendationServiceClient recommendationServiceClient;

    public CustomerProfileService(OrderServiceClient orderServiceClient,
                                  RecommendationServiceClient recommendationServiceClient) {
        this.orderServiceClient = orderServiceClient;
        this.recommendationServiceClient = recommendationServiceClient;
    }

    public CustomerProfile getProfile(String customerId) throws OrderServiceException, RecommendationServiceException {
        try {
            return ScopedValue.where(CUSTOMER_ID, customerId).call(() -> {
                String contextCustomerId = CUSTOMER_ID.get();
                CompletableFuture<List<Order>> orderFuture =
                        CompletableFuture.supplyAsync(() -> orderServiceClient.getOrders(contextCustomerId), executor);
                CompletableFuture<List<Recommendation>> recFuture =
                        CompletableFuture.supplyAsync(() -> recommendationServiceClient.getRecommendations(contextCustomerId), executor);

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(orderFuture, recFuture);

        try {
            allFutures.get(2, TimeUnit.SECONDS);
            return new CustomerProfile(customerId, orderFuture.join(), recFuture.join());
        }

        catch (ExecutionException e) {
            Throwable cause = e.getCause();
            orderFuture.cancel(true);
            recFuture.cancel(true);

                    if (cause instanceof RestClientResponseException ex && ex.getStatusCode().value() == 503) {
                        if (orderFuture.isCompletedExceptionally()) {
                            throw new OrderServiceException("Order service unavailable", e.getCause());
                        }
                        if (recFuture.isCompletedExceptionally()) {
                            throw new RecommendationServiceException("Recommendation service unavailable", e.getCause());
                        }
                    }
                    throw new RuntimeException("Unexpected error", e.getCause());
                } catch (TimeoutException e) {
                    orderFuture.cancel(true);
                    recFuture.cancel(true);
                    throw new RuntimeException("Request timed out");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted", e);
                }
            });
        } catch (OrderServiceException | RecommendationServiceException | RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
