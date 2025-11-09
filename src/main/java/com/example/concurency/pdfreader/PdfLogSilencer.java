package com.example.concurency.pdfreader;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility to silence noisy PDFBox font-mapping warnings like:
 * "No Unicode mapping for parenleftBig in font ZMSWIG+CMEX10".
 *
 * Safe to use globally. Does not affect actual PDF text extraction.
 */
public final class PdfLogSilencer {

    private PdfLogSilencer() {
        // Prevent instantiation
    }

    /**
     * Call this once at application or test startup.
     * It suppresses PDFBox font-related warnings.
     */
    public static void silencePdfBoxWarnings() {
        // Silence only the font mapping warnings
        Logger.getLogger("org.apache.pdfbox.pdmodel.font.PDSimpleFont")
            .setLevel(Level.SEVERE);

        // Optionally silence other chatty PDFBox areas too
        Logger.getLogger("org.apache.pdfbox.pdmodel.font.PDFont")
            .setLevel(Level.SEVERE);
        Logger.getLogger("org.apache.pdfbox.pdmodel.font.FileSystemFontProvider")
            .setLevel(Level.SEVERE);
    }
}
