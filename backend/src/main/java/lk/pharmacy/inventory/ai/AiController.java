package lk.pharmacy.inventory.ai;

import jakarta.validation.Valid;
import lk.pharmacy.inventory.ai.dto.AiChatRequest;
import lk.pharmacy.inventory.ai.dto.AiChatResponse;
import lk.pharmacy.inventory.ai.dto.AiQueryRequest;
import lk.pharmacy.inventory.tenant.TenantFeatureGuardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai")
public class AiController {

    private final AiAssistantService aiAssistantService;
    private final TenantFeatureGuardService tenantFeatureGuardService;

    public AiController(AiAssistantService aiAssistantService,
                        TenantFeatureGuardService tenantFeatureGuardService) {
        this.aiAssistantService = aiAssistantService;
        this.tenantFeatureGuardService = tenantFeatureGuardService;
    }

    @PostMapping("/chat")
    @PreAuthorize("hasAnyRole('ADMIN','BILLING','INVENTORY','TRANSACTIONS')")
    public AiChatResponse chat(@Valid @RequestBody AiChatRequest request) {
        tenantFeatureGuardService.requireAiEnabled();
        return aiAssistantService.chat(request);
    }

    @PostMapping("/query")
    @PreAuthorize("hasAnyRole('ADMIN','BILLING','INVENTORY','TRANSACTIONS')")
    public Map<String, Object> query(@Valid @RequestBody AiQueryRequest request) {
        tenantFeatureGuardService.requireAiEnabled();
        AiChatResponse response = aiAssistantService.chat(new AiChatRequest(request.query(), null, List.of()));
        return Map.of(
                "intent", response.intent(),
                "message", response.answer(),
                "quickActions", response.quickActions(),
                "data", response.data()
        );
    }
}
