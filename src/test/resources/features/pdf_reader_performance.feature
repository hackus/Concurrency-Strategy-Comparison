@pdfreader
Feature: PDF Reader Performance Comparison
  This feature compares multiple concurrency strategies for reading and extracting text from PDFs.

  Background:
    Given a PDF file "combinepdf-1.pdf" and expected output "expected_extracted_text.txt"
    And any existing "extracted_text.txt" file is deleted
    And PDFBox warnings are silenced

  Scenario Outline: Run PDF Reader with <strategy>
    When I run the "<strategy>"
    Then the output text should match the expected text
    Then the run should complete within <timeout> ms

    Examples:
      | strategy                                  | timeout |
      | FuturePdfReaderDemo                       | 2500    |
      | CompletableFuturePdfReaderBlockingDemo    | 2500    |
      | VirtualThreadPdfReaderDemo                | 2500    |
      | CompletableFuturePdfReaderNonBlockingDemo | 2500    |
      | RxJavaPdfReaderDemo                       | 2500    |
