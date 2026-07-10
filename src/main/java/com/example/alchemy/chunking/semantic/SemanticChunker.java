package com.example.alchemy.chunking.semantic;

import com.example.alchemy.chunking.config.ChunkingConfig;
import com.example.alchemy.chunking.model.Chunk;
import com.example.alchemy.chunking.model.TextElement;
import com.example.alchemy.chunking.token.TokenCounter;
import java.util.List;

/**
 * Interface to segment structured document elements into token-bounded semantic chunks.
 */
public interface SemanticChunker {
    /**
     * Chunks a list of structured TextElements according to config constraints.
     * @param elements The structured text elements.
     * @param config The chunking parameters.
     * @param tokenCounter The token estimator.
     * @return A list of generated chunks.
     */
    List<Chunk> chunk(List<TextElement> elements, ChunkingConfig config, TokenCounter tokenCounter);
}
