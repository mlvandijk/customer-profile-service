package com.maritvandijk.profileservice.model;

import java.util.List;

public record CustomerProfile(String customerId, List<Order> orders, List<Recommendation> recommendations) {}
