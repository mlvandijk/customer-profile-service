package com.maritvandijk.profileservice.controller;

import com.maritvandijk.profileservice.exception.OrderServiceException;
import com.maritvandijk.profileservice.exception.RecommendationServiceException;
import com.maritvandijk.profileservice.model.CustomerProfile;
import com.maritvandijk.profileservice.service.CustomerProfileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CustomerProfileController {

    private final CustomerProfileService customerProfileService;

    public CustomerProfileController(CustomerProfileService customerProfileService) {
        this.customerProfileService = customerProfileService;
    }

    @GetMapping("/profile/{customerId}")
    public CustomerProfile getProfile(@PathVariable String customerId) throws OrderServiceException, RecommendationServiceException {
        return customerProfileService.getProfile(customerId);
    }
}
