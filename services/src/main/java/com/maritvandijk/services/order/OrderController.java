package com.maritvandijk.services.order;

import com.maritvandijk.services.exception.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final int delayMs;

    public OrderController(@Value("${downstream.delay-ms:200}") int delayMs) {
        this.delayMs = delayMs;
    }

    @GetMapping("/orders/{customerId}")
    public List<Order> getOrders(@PathVariable String customerId,
                                 @RequestParam(defaultValue = "false") boolean fail) throws InterruptedException {
        log.info("Handling /orders/{} on {}", customerId, Thread.currentThread());
        if (fail) {
            Thread.sleep(delayMs);
            throw new ServiceUnavailableException("Order service unavailable");
        }
        Thread.sleep(delayMs);
        return List.of(new Order("order-1", "Widget"), new Order("order-2", "Gadget"));
    }
}
