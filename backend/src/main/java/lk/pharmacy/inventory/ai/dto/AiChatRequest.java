package lk.pharmacy.inventory.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record AiChatRequest(
        @NotBlank String query,
        String sessionId,
        @Valid List<AiChatMessage> history
) {
}

