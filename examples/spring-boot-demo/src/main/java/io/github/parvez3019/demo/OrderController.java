package io.github.parvez3019.demo;

import io.github.parvez3019.spring.RequestLogger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /orders/{id}  → happy path: INFO/DEBUG lines are buffered and discarded, nothing is written.
 * GET /orders/fail  → payment fails: buffered lines are flushed together with the ERROR.
 */
@RestController
public class OrderController {

    @GetMapping("/orders/{id}")
    public ResponseEntity<String> order(@PathVariable String id) {
        RequestLogger.get().info("Fetching order {}", id);
        RequestLogger.get().info("Order {} has 3 items, total=42.50 EUR", id);
        RequestLogger.get().debug("Cache miss for customer profile, loading from DB");
        RequestLogger.get().info("Calling payment provider for order {}", id);
        try {
            charge(id);
        } catch (IllegalStateException ex) {
            RequestLogger.get().error("Payment failed for order {}", id, ex);
            return ResponseEntity.status(502).body("payment failed\n");
        }
        return ResponseEntity.ok("order " + id + " paid\n");
    }

    private void charge(String id) {
        if ("fail".equals(id)) {
            throw new IllegalStateException("card declined (code 51)");
        }
    }
}
