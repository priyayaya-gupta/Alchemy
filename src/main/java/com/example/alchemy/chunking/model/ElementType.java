package com.example.alchemy.chunking.model;

/**
 * Represents the logical structure type of a parsed document element.
 */
public enum ElementType {
    HEADING,
    PARAGRAPH,
    LIST_ITEM,
    TABLE,
    CODE_BLOCK,
    FAQ,
    PAGE_BREAK,
    OCR_NOISE,
    UNKNOWN
}
