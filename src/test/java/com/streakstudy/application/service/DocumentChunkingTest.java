package com.streakstudy.application.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentChunkingTest {

    @Test
    void textoCorto_produceSoloUnChunk() {
        String texto = "Párrafo uno.\n\nPárrafo dos.";
        List<String> chunks = DocumentService.chunk(texto, 2000);
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).contains("Párrafo uno.");
    }

    @Test
    void textoPorEncimaDeLimite_seParticionaEnVariosChunks() {
        String parrafoGrande = "x".repeat(1500);
        String texto = parrafoGrande + "\n\n" + parrafoGrande;
        List<String> chunks = DocumentService.chunk(texto, 2000);
        assertThat(chunks).hasSize(2);
    }

    @Test
    void lineasBlancasMultiples_seIgnoran() {
        String texto = "Párrafo A.\n\n\n\n\nPárrafo B.";
        List<String> chunks = DocumentService.chunk(texto, 2000);
        assertThat(chunks).hasSize(1);
    }

    @Test
    void textoVacio_retornaListaVacia() {
        List<String> chunks = DocumentService.chunk("   \n\n   ", 2000);
        assertThat(chunks).isEmpty();
    }

    @Test
    void parrafosIndividualesQueSuperanElLimite_cadaUnoEsSuPropioCHunk() {
        String parrafo = "y".repeat(2500);
        String texto = parrafo + "\n\n" + parrafo;
        List<String> chunks = DocumentService.chunk(texto, 2000);
        assertThat(chunks).hasSize(2);
        chunks.forEach(c -> assertThat(c).hasSizeGreaterThan(2000));
    }
}
