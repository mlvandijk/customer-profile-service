package com.maritvandijk.profileservice.client;

import com.maritvandijk.profileservice.model.Recommendation;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.List;

@HttpExchange
public interface RecommendationServiceClient {

    @GetExchange("/recommendations/{customerId}")
    List<Recommendation> getRecommendations(@PathVariable String customerId);
}
