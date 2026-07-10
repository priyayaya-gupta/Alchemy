package com.example.alchemy.chunking.parser;

import com.example.alchemy.chunking.model.TextElement;
import java.util.List;

/**
 * Interface to parse raw document text into logical blocks (TextElements).
 */
public interface DocumentParser {
    /**
     * Parses a raw text document into a list of TextElements.
     * @param text The raw document string.
     * @return A list of initial text elements.
     */
    List<TextElement> parse(String text);
}
