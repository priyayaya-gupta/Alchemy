package com.example.alchemy.chunking.overlap;

import com.example.alchemy.chunking.config.ChunkingConfig;
import com.example.alchemy.chunking.model.Chunk;
import com.example.alchemy.chunking.token.TokenCounter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements semantic overlap by repeating the last N sentences of the previous chunk,
 * skipping structural components like tables and code blocks to avoid corruption.
 */
@Component
public class SemanticOverlapProcessor implements OverlapProcessor {

    private final TokenCounter tokenCounter;

    public SemanticOverlapProcessor(TokenCounter tokenCounter) {
        this.tokenCounter = tokenCounter;
    }

    @Override
    public List<Chunk> processOverlap(List<Chunk> chunks, ChunkingConfig config) {
        if (chunks == null || chunks.isEmpty()) {
            return new ArrayList<>();
        }

        int overlapSentences = config.getOverlapSentences();
        if (overlapSentences <= 0) {
            return chunks;
        }

        for (int i = 0; i < chunks.size(); i++) {
            Chunk current = chunks.get(i);

            if (i > 0) {
                Chunk previous = chunks.get(i - 1);
                String prevType = previous.getMetadata().getChunkType();

                // Do not pull overlap from code blocks or tables to maintain structure integrity
                if (!"TABLE".equals(prevType) && !"CODE_BLOCK".equals(prevType)) {
                    String overlapText = extractLastSentences(previous.getText(), overlapSentences);
                    if (!overlapText.isBlank()) {
                        current.setOverlapText(overlapText);
                    }
                }
            }

            // Always update final token count including overlap
            int totalTokens = tokenCounter.countTokens(current.getFullTextWithOverlap());
            current.getMetadata().setTokenCount(totalTokens);
        }

        return chunks;
    }

    private String extractLastSentences(String text, int count) {
        if (text == null || text.isBlank()) {
            return "";
        }

        // Split text by sentence boundaries (periods, exclamation marks, question marks followed by spaces)
        String[] sentences = text.split("(?<=[.!?])\\s+");
        if (sentences.length == 0) {
            return "";
        }

        int start = Math.max(0, sentences.length - count);
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < sentences.length; i++) {
            String trimmed = sentences[i].trim();
            if (!trimmed.isEmpty()) {
                sb.append(trimmed).append(" ");
            }
        }

        return sb.toString().trim();
    }
}
