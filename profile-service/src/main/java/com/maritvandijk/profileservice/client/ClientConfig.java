package com.maritvandijk.profileservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class ClientConfig {

    @Value("${services.base-url}")
    private String baseUrl;

    @Bean
    RestClient restClient() {
        return RestClient.builder().baseUrl(baseUrl).build();
    }

    @Bean
    HttpServiceProxyFactory proxyFactory(RestClient restClient) {
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient)).build();
    }

    @Bean
    OrderServiceClient orderServiceClient(HttpServiceProxyFactory f) {
        return f.createClient(OrderServiceClient.class);
    }

    @Bean
    RecommendationServiceClient recommendationServiceClient(HttpServiceProxyFactory f) {
        return f.createClient(RecommendationServiceClient.class);
    }
}

