package com.maritvandijk.profileservice.client;

import com.maritvandijk.profileservice.model.Order;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.List;

@HttpExchange
public interface OrderServiceClient {

    @GetExchange("/orders/{customerId}")
    List<Order> getOrders(@PathVariable String customerId);
}
