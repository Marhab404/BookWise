package com.bookwise.chatbot.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GeminiClient {

    private final RestClient restClient;
    private final String model;

    @Autowired
    public GeminiClient(
            @Value("${app.gemini.api-key}") String apiKey,
            @Value("${app.gemini.model:gemini-3.1-flash-lite}") String model
    ) {
        String cleanKey = apiKey;
        if (cleanKey != null) {
            cleanKey = cleanKey.trim();
            if (cleanKey.startsWith("'") && cleanKey.endsWith("'")) {
                cleanKey = cleanKey.substring(1, cleanKey.length() - 1);
            } else if (cleanKey.startsWith("\"") && cleanKey.endsWith("\"")) {
                cleanKey = cleanKey.substring(1, cleanKey.length() - 1);
            }
        }
        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .defaultHeader("x-goog-api-key", cleanKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.model = model;
    }

    // Constructor with custom RestClient for testing
    public GeminiClient(RestClient restClient, String model) {
        this.restClient = restClient;
        this.model = model;
    }

    public InteractionResponse postInteraction(String input, String previousInteractionId) {
        String nextPrevId = (previousInteractionId != null && !previousInteractionId.isBlank()) ? previousInteractionId : null;
        InteractionRequest requestBody = new InteractionRequest(model, input, nextPrevId);
        
        return restClient.post()
                .uri("/v1beta/interactions")
                .body(requestBody)
                .retrieve()
                .body(InteractionResponse.class);
    }

    public static String extractTextResponse(InteractionResponse response) {
        if (response == null || response.steps() == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Step step : response.steps()) {
            if ("model_output".equals(step.type()) && step.content() != null) {
                for (Content c : step.content()) {
                    if ("text".equals(c.type()) && c.text() != null) {
                        sb.append(c.text());
                    }
                }
            }
        }
        return sb.toString();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record InteractionRequest(
            String model,
            String input,
            String previous_interaction_id
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record InteractionResponse(
            String id,
            String status,
            List<Step> steps
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Step(
            String type,
            List<Content> content
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Content(
            String type,
            String text
    ) {}
}
