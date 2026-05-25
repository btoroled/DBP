package com.streakstudy.infrastructure.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streakstudy.application.port.AiFlashcardGeneratorPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class AnthropicFlashcardGeneratorAdapter implements AiFlashcardGeneratorPort {

    public static final String MODEL    = "claude-haiku-4-5-20251001";
    public static final String PROVIDER = "anthropic";
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String SYSTEM_PROMPT = """
            Eres un asistente que genera flashcards educativas a partir de texto académico.
            Dado un fragmento de texto, devuelve ÚNICAMENTE un array JSON con objetos que tengan
            los campos "question" y "answer". Sin texto adicional, solo el JSON.
            Genera entre 2 y 5 flashcards. Las respuestas deben ser concisas (máx 80 palabras).
            Ejemplo: [{"question":"¿Qué es X?","answer":"X es..."}]
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.anthropic.api-key}")
    private String apiKey;

    public AnthropicFlashcardGeneratorAdapter(ObjectMapper objectMapper) {
        this.restClient = RestClient.create();
        this.objectMapper = objectMapper;
    }

    @Override
    public GenerationResult generate(String textChunk) {
        Map<String, Object> body = Map.of(
                "model", MODEL,
                "max_tokens", 1024,
                "system", SYSTEM_PROMPT,
                "messages", List.of(Map.of("role", "user", "content", textChunk))
        );

        String raw = restClient.post()
                .uri(API_URL)
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        return parseResponse(raw);
    }

    private GenerationResult parseResponse(String raw) {
        try {
            JsonNode root = objectMapper.readTree(raw);
            String content = root.path("content").get(0).path("text").asText();
            content = content.replaceAll("(?s)```json\\s*|```", "").strip();
            List<FlashcardSuggestion> flashcards = objectMapper.readValue(content, new TypeReference<>() {});

            int inputTokens  = root.path("usage").path("input_tokens").asInt(0);
            int outputTokens = root.path("usage").path("output_tokens").asInt(0);

            return new GenerationResult(flashcards, inputTokens, outputTokens);
        } catch (Exception e) {
            throw new RuntimeException("Error al parsear respuesta de Anthropic: " + e.getMessage(), e);
        }
    }
}
