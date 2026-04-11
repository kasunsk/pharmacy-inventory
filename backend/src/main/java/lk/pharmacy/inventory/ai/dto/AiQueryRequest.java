package lk.pharmacy.inventory.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record AiQueryRequest(@NotBlank String query) {
}

