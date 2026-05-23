package com.streakstudy.application.port;

import java.util.List;

public interface AiFlashcardGeneratorPort {

    record FlashcardSuggestion(String question, String answer) {}

    record GenerationResult(List<FlashcardSuggestion> flashcards, int inputTokens, int outputTokens) {}

    GenerationResult generate(String textChunk);
}
