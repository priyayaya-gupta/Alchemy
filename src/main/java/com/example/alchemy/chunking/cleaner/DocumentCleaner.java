package com.example.alchemy.chunking.cleaner;

import com.example.alchemy.chunking.model.TextElement;
import java.util.List;

/**
 * Interface to clean OCR noise, repeated headers/footers, and fix line breaks or broken words.
 */
public interface DocumentCleaner {
    /**
     * Cleans the parsed text elements.
     * @param elements The list of parsed text elements.
     * @return The cleaned list of text elements.
     */
    List<TextElement> clean(List<TextElement> elements);
}
