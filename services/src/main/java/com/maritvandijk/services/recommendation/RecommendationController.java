package com.maritvandijk.services.recommendation;

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
public class RecommendationController {

    private static final Logger log = LoggerFactory.getLogger(RecommendationController.class);

    private final int delayMs;

    public RecommendationController(@Value("${downstream.delay-ms:200}") int delayMs) {
        this.delayMs = delayMs;
    }

    @GetMapping("/recommendations/{customerId}")
    public List<Recommendation> getRecommendations(@PathVariable String customerId,
                                                    @RequestParam(defaultValue = "false") boolean fail) throws InterruptedException {
        log.info("Handling /recommendations/{} on {}", customerId, Thread.currentThread());
        if (fail) {
            Thread.sleep(delayMs);
            throw new ServiceUnavailableException("Recommendation service unavailable");
        }
        Thread.sleep(delayMs);
        return List.of(new Recommendation("rec-1", "Super Widget"), new Recommendation("rec-2", "Mega Gadget"));
    }
}
