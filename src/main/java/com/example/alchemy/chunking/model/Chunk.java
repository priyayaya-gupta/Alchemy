package com.example.alchemy.chunking.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single text chunk with optional overlap context and metadata.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Chunk {
    private String text;
    private String overlapText;
    private ChunkMetadata metadata;

    /**
     * Combines the semantic overlap and core chunk content for indexing/embedding.
     */
    public String getFullTextWithOverlap() {
        if (overlapText == null || overlapText.isBlank()) {
            return text;
        }
        return overlapText + "\n" + text;
    }
}
