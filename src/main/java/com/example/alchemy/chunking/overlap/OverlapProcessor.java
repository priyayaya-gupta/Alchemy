package com.example.alchemy.chunking.overlap;

import com.example.alchemy.chunking.config.ChunkingConfig;
import com.example.alchemy.chunking.model.Chunk;
import java.util.List;

/**
 * Interface to inject overlap context into consecutive document chunks.
 */
public interface OverlapProcessor {
    /**
     * Enhances a list of chunks with semantic overlap from previous chunks.
     * @param chunks The list of raw chunks.
     * @param config The chunking parameters.
     * @return A list of chunks with overlap processed.
     */
    List<Chunk> processOverlap(List<Chunk> chunks, ChunkingConfig config);
}
