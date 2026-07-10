package com.example.alchemy.chunking.parser;

import com.example.alchemy.chunking.model.ElementType;
import com.example.alchemy.chunking.model.TextElement;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Line-by-line document parser that groups text into paragraphs but isolates headings,
 * code blocks, and tables, respecting page feeds (\f).
 */
@Component
public class DefaultDocumentParser implements DocumentParser {

    @Override
    public List<TextElement> parse(String text) {
        List<TextElement> elements = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return elements;
        }

        // Normalize carriage returns
        String normalized = text.replace("\r\n", "\n").replace("\r", "\n");
        String[] lines = normalized.split("\n");

        StringBuilder currentParagraph = new StringBuilder();
        int currentPage = 1;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();

            // Handle page breaks
            if (trimmed.contains("\f")) {
                yieldParagraph(elements, currentParagraph, currentPage);
                
                String[] pages = line.split("\\f");
                for (int p = 0; p < pages.length; p++) {
                    String pageText = pages[p].trim();
                    if (!pageText.isEmpty()) {
                        elements.add(TextElement.builder()
                                .content(pageText)
                                .type(ElementType.UNKNOWN)
                                .pageNumber(currentPage)
                                .build());
                    }
                    if (p < pages.length - 1) {
                        currentPage++;
                    }
                }
                continue;
            }

            // Headings
            if (isHeadingLine(trimmed)) {
                yieldParagraph(elements, currentParagraph, currentPage);
                elements.add(TextElement.builder()
                        .content(trimmed)
                        .type(ElementType.UNKNOWN)
                        .pageNumber(currentPage)
                        .build());
                continue;
            }

            // Code Blocks
            if (trimmed.startsWith("```")) {
                yieldParagraph(elements, currentParagraph, currentPage);
                StringBuilder codeBlock = new StringBuilder(line).append("\n");
                i++;
                while (i < lines.length) {
                    codeBlock.append(lines[i]).append("\n");
                    if (lines[i].trim().startsWith("```")) {
                        break;
                    }
                    i++;
                }
                elements.add(TextElement.builder()
                        .content(codeBlock.toString().trim())
                        .type(ElementType.UNKNOWN)
                        .pageNumber(currentPage)
                        .build());
                continue;
            }

            // Tables
            if (trimmed.startsWith("|")) {
                yieldParagraph(elements, currentParagraph, currentPage);
                StringBuilder table = new StringBuilder(line).append("\n");
                i++;
                while (i < lines.length && lines[i].trim().startsWith("|")) {
                    table.append(lines[i]).append("\n");
                    i++;
                }
                i--;
                elements.add(TextElement.builder()
                        .content(table.toString().trim())
                        .type(ElementType.UNKNOWN)
                        .pageNumber(currentPage)
                        .build());
                continue;
            }

            // General lines (accumulate into paragraph)
            if (trimmed.isEmpty()) {
                yieldParagraph(elements, currentParagraph, currentPage);
            } else {
                if (currentParagraph.length() > 0) {
                    currentParagraph.append("\n");
                }
                currentParagraph.append(line);
            }
        }

        yieldParagraph(elements, currentParagraph, currentPage);

        return elements;
    }

    private void yieldParagraph(List<TextElement> elements, StringBuilder paragraph, int page) {
        if (paragraph.length() > 0) {
            String content = paragraph.toString().trim();
            if (!content.isEmpty()) {
                elements.add(TextElement.builder()
                        .content(content)
                        .type(ElementType.UNKNOWN)
                        .pageNumber(page)
                        .build());
            }
            paragraph.setLength(0);
        }
    }

    private boolean isHeadingLine(String trimmed) {
        if (trimmed.matches("^#{1,6}\\s+.*")) {
            return true;
        }
        if (trimmed.matches("^<h[1-6]>.*</h[1-6]>$")) {
            return true;
        }
        return false;
    }
}
