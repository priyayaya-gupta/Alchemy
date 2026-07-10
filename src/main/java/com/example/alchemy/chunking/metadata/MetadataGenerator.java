package com.example.alchemy.chunking.metadata;

import com.example.alchemy.chunking.model.Chunk;
import java.util.List;

/**
 * Interface to finalize chunk metadata with document tracking details and chunk indexes.
 */
public interface MetadataGenerator {
    /**
     * Finalizes metadata for a list of processed chunks.
     * @param chunks The list of chunks.
     * @param documentName The name of the source document.
     * @param documentId The unique ID of the document.
     * @return The enriched list of chunks.
     */
    List<Chunk> generateMetadata(List<Chunk> chunks, String documentName, String documentId);
}
