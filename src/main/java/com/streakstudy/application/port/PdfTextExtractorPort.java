package com.streakstudy.application.port;

import java.io.InputStream;

public interface PdfTextExtractorPort {
    String extract(InputStream pdfStream);
}
