package com.example.concurrency.pdfreader;

import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;

public class SafeTextStripper extends PDFTextStripper {
    public SafeTextStripper() throws IOException {}

    @Override
    protected void processTextPosition(org.apache.pdfbox.text.TextPosition text) {
        try {
            super.processTextPosition(text);
        } catch (Exception e) {
        }
    }
}