package com.streakstudy.application.port;

import java.util.List;

public interface AiFlashcardGeneratorPort {

    record FlashcardSuggestion(String question, String answer) {}

    List<FlashcardSuggestion> generate(String textChunk);
}
