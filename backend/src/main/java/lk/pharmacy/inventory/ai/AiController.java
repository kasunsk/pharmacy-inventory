package lk.pharmacy.inventory.ai;

import jakarta.validation.Valid;
import lk.pharmacy.inventory.ai.dto.AiQueryRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/ai")
public class AiController {

    private final AnalyticsService analyticsService;

    public AiController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @PostMapping("/query")
    @PreAuthorize("hasAnyRole('ADMIN','TRANSACTIONS')")
    public Map<String, Object> query(@Valid @RequestBody AiQueryRequest request) {
        String normalized = request.query().trim().toLowerCase();

        if (normalized.contains("low") && normalized.contains("stock")) {
            return analyticsService.lowStock();
        }
        if (normalized.contains("today") && normalized.contains("sales")) {
            return analyticsService.todaySales();
        }
        if (normalized.contains("selling") && normalized.contains("most")) {
            return analyticsService.topSelling();
        }
        if (normalized.contains("do we have ")) {
            String name = normalized.replace("do we have", "").replace("?", "").trim();
            return analyticsService.availability(name);
        }

        return Map.of(
                "intent", "unknown",
                "message", "Try: low stock, today's total sales, most selling medicine, or do we have <name>?"
        );
    }
}

