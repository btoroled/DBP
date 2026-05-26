package com.streakstudy.infrastructure.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streakstudy.application.port.AiFlashcardGeneratorPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Fallback de {@link AiFlashcardGeneratorPort} que NO consume la API de Anthropic.
 *
 * <p>Se activa cuando {@code app.ai.anthropic.api-key} esta vacio. Util para demos
 * MVP, dev local y CI sin gastar tokens. Devuelve un JSON con la misma forma que
 * produciria el adapter real, extrayendo oraciones del chunk como respuestas
 * para que la salida se sienta ligada al PDF subido.</p>
 */
@Component
@ConditionalOnExpression("'${app.ai.anthropic.api-key:}' == ''")
public class StubFlashcardGeneratorAdapter implements AiFlashcardGeneratorPort {

    public static final String MODEL    = "stub-flashcard-generator";
    public static final String PROVIDER = "stub";

    private static final Logger log = LoggerFactory.getLogger(StubFlashcardGeneratorAdapter.class);

    private static final int MIN_SENTENCE_LEN = 30;
    private static final int MAX_SENTENCE_LEN = 300;
    private static final int MAX_FLASHCARDS   = 3;

    private final ObjectMapper objectMapper;

    public StubFlashcardGeneratorAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public GenerationResult generate(String textChunk) {
        List<FlashcardSuggestion> flashcards = buildFlashcards(textChunk);

        int inputTokens  = estimateTokens(textChunk);
        int outputTokens = flashcards.stream()
                .mapToInt(f -> estimateTokens(f.question()) + estimateTokens(f.answer()))
                .sum();

        log.info("[AI-STUB] chunk_len={} -> {} flashcards (in={} out={}): {}",
                textChunk.length(), flashcards.size(), inputTokens, outputTokens, toJson(flashcards));

        return new GenerationResult(flashcards, inputTokens, outputTokens);
    }

    private List<FlashcardSuggestion> buildFlashcards(String chunk) {
        List<String> sentences = extractSentences(chunk);
        List<FlashcardSuggestion> result = new ArrayList<>();

        int target = Math.min(MAX_FLASHCARDS, Math.max(2, sentences.size()));
        for (int i = 0; i < target && i < sentences.size(); i++) {
            String sentence = sentences.get(i);
            String topic = extractTopic(sentence);
            result.add(new FlashcardSuggestion(
                    "¿Que se menciona sobre " + topic + "?",
                    sentence
            ));
        }
        if (result.isEmpty()) {
            String preview = chunk.length() > 200 ? chunk.substring(0, 200) + "..." : chunk;
            result.add(new FlashcardSuggestion(
                    "¿Cual es el contenido del fragmento?",
                    preview.isBlank() ? "Fragmento sin contenido textual util." : preview
            ));
        }
        return result;
    }

    private List<String> extractSentences(String text) {
        List<String> sentences = new ArrayList<>();
        for (String s : text.split("(?<=[.!?])\\s+")) {
            String trimmed = s.strip();
            if (trimmed.length() >= MIN_SENTENCE_LEN && trimmed.length() <= MAX_SENTENCE_LEN) {
                sentences.add(trimmed);
            }
        }
        return sentences;
    }

    private String extractTopic(String sentence) {
        String[] words = sentence.replaceAll("[^\\p{L}\\s]", "").split("\\s+");
        for (String w : words) {
            if (w.length() > 4 && Character.isUpperCase(w.charAt(0))) {
                return w.toLowerCase();
            }
        }
        for (String w : words) {
            if (w.length() > 5) return w.toLowerCase();
        }
        return "el tema";
    }

    private int estimateTokens(String text) {
        return Math.max(1, text.length() / 4);
    }

    private String toJson(List<FlashcardSuggestion> flashcards) {
        try {
            return objectMapper.writeValueAsString(flashcards);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}
