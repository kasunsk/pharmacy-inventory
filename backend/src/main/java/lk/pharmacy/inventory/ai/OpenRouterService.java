package lk.pharmacy.inventory.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class OpenRouterService {

    private final String apiKey;
    private final String model;
    private final String endpoint;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenRouterService(@Value("${app.ai.openrouter.api-key:${OPENROUTER_API_KEY:}}") String apiKey,
                             @Value("${app.ai.openrouter.model:openai/gpt-4o-mini}") String model,
                             @Value("${app.ai.openrouter.endpoint:https://openrouter.ai/api/v1/chat/completions}") String endpoint,
                             ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.model = model;
        this.endpoint = endpoint;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String complete(String systemPrompt, String userPrompt) {
        if (!isEnabled()) {
            return "AI language model is not configured; using built-in assistant response.";
        }

        try {
            Map<String, Object> payload = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    ),
                    "temperature", 0.2
            );

            String body = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return "I could not reach the AI model right now, but I can still help with system data.";
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.asText().isBlank()) {
                return "I could not generate a detailed AI explanation right now.";
            }
            return content.asText();
        } catch (Exception ex) {
            return "I could not reach the AI model right now, but I can still help with system data.";
        }
    }
}

