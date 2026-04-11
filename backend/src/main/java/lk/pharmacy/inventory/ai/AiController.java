package lk.pharmacy.inventory.ai;

import jakarta.validation.Valid;
import lk.pharmacy.inventory.ai.dto.AiChatRequest;
import lk.pharmacy.inventory.ai.dto.AiChatResponse;
import lk.pharmacy.inventory.ai.dto.AiQueryRequest;
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

    public AiController(AiAssistantService aiAssistantService) {
        this.aiAssistantService = aiAssistantService;
    }

    @PostMapping("/chat")
    @PreAuthorize("hasAnyRole('ADMIN','BILLING','INVENTORY','TRANSACTIONS')")
    public AiChatResponse chat(@Valid @RequestBody AiChatRequest request) {
        return aiAssistantService.chat(request);
    }

    @PostMapping("/query")
    @PreAuthorize("hasAnyRole('ADMIN','BILLING','INVENTORY','TRANSACTIONS')")
    public Map<String, Object> query(@Valid @RequestBody AiQueryRequest request) {
        AiChatResponse response = aiAssistantService.chat(new AiChatRequest(request.query(), null, List.of()));
        return Map.of(
                "intent", response.intent(),
                "message", response.answer(),
                "quickActions", response.quickActions(),
                "data", response.data()
        );
    }
}
