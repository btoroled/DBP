package com.streakstudy.infrastructure.ai;

import com.streakstudy.application.port.PdfTextExtractorPort;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
public class PdfBoxTextExtractorAdapter implements PdfTextExtractorPort {

    @Override
    public String extract(InputStream pdfStream) {
        try (PDDocument doc = Loader.loadPDF(pdfStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String raw = stripper.getText(doc);
            return clean(raw);
        } catch (Exception e) {
            throw new RuntimeException("Error al extraer texto del PDF", e);
        }
    }

    private String clean(String text) {
        return text
                .replaceAll("(?m)^\\s*\\d+\\s*$", "")       // elimina líneas que solo son números de página
                .replaceAll("[ \\t]{2,}", " ")                // colapsa espacios múltiples
                .replaceAll("(?m)^[ \\t]+", "")               // elimina indentación
                .replaceAll("\n{3,}", "\n\n")                  // máximo una línea en blanco entre párrafos
                .strip();
    }
}
