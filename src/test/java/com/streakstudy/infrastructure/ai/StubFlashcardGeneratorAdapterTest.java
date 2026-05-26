package com.streakstudy.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streakstudy.application.port.AiFlashcardGeneratorPort.GenerationResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StubFlashcardGeneratorAdapterTest {

    private final StubFlashcardGeneratorAdapter stub =
            new StubFlashcardGeneratorAdapter(new ObjectMapper());

    @Test
    void generate_chunkConOraciones_devuelveFlashcardsExtractivas() {
        String chunk = """
                La fotosintesis es el proceso por el cual las plantas convierten luz solar en energia quimica.
                Las plantas absorben dioxido de carbono del aire y agua del suelo a traves de las raices.
                El resultado de este proceso es la produccion de glucosa y oxigeno como subproducto.
                """;

        GenerationResult result = stub.generate(chunk);

        assertThat(result.flashcards()).hasSizeBetween(2, 3);
        assertThat(result.flashcards()).allSatisfy(f -> {
            assertThat(f.question()).startsWith("¿");
            assertThat(f.answer()).isNotBlank();
        });
        assertThat(result.inputTokens()).isGreaterThan(0);
        assertThat(result.outputTokens()).isGreaterThan(0);
    }

    @Test
    void generate_chunkVacioOMuyCorto_devuelveAlMenosUnaFlashcard() {
        GenerationResult result = stub.generate("Texto corto.");

        assertThat(result.flashcards()).isNotEmpty();
        assertThat(result.flashcards().get(0).answer()).isNotBlank();
    }

    @Test
    void generate_chunkLargo_limitaA3Flashcards() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            sb.append("Esta es la oracion numero ").append(i)
              .append(" con suficiente longitud para ser considerada valida por el extractor. ");
        }

        GenerationResult result = stub.generate(sb.toString());

        assertThat(result.flashcards()).hasSize(3);
    }
}
