package com.example.alchemy.chunking.structure;

import com.example.alchemy.chunking.model.TextElement;
import java.util.List;

/**
 * Interface to detect document structural components (headings, tables, lists, code blocks, FAQs).
 */
public interface StructureDetector {
    /**
     * Identifies layout structure and assigns types and section hierarchy to each element.
     * @param elements The list of clean text elements.
     * @return A list of structured elements.
     */
    List<TextElement> detectStructure(List<TextElement> elements);
}
