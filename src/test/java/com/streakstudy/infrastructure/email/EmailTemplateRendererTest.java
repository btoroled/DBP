package com.streakstudy.infrastructure.email;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

class EmailTemplateRendererTest {

    private EmailTemplateRenderer renderer;

    @BeforeEach
    void setup() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);

        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);

        renderer = new EmailTemplateRenderer(engine);
    }

    @Test
    void shouldRenderFullNameInWelcomeTemplate() {
        String html = renderer.render("welcome",
                Map.of("fullName", "Alice Perez", "email", "alice@utec.edu"));

        assertThat(html).contains("Alice Perez");
        assertThat(html).contains("alice@utec.edu");
        assertThat(html).contains("Bienvenido a StreakStudy");
    }

    @Test
    void shouldRenderFlashcardCountInReadyTemplate() {
        String html = renderer.render("flashcards-ready",
                Map.of("fullName", "Bob", "flashcardCount", 17, "deckId", 42L, "jobId", 7L));

        assertThat(html).contains("Bob");
        assertThat(html).contains("17");
        assertThat(html).contains("42");
    }

    @Test
    void shouldRenderBadgeDisplayNameInBadgeTemplate() {
        String html = renderer.render("badge-earned",
                Map.of("fullName", "Carla",
                       "badgeDisplayName", "Estrella de Racha",
                       "badgeDescription", "Por alcanzar una racha de 3 dias"));

        assertThat(html).contains("Carla");
        assertThat(html).contains("Estrella de Racha");
        assertThat(html).contains("Por alcanzar una racha de 3 dias");
    }
}
