package lk.pharmacy.inventory.ai.dto;

import java.util.List;
import java.util.Map;

public record AiChatResponse(
        String intent,
        String answer,
        List<String> quickActions,
        Map<String, Object> data
) {
}

