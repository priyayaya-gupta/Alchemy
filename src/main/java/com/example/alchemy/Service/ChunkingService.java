package com.example.alchemy.Service;

import com.example.alchemy.chunking.model.Chunk;
import com.example.alchemy.chunking.pipeline.ChunkingPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ChunkingService {

    private static final Logger log = LoggerFactory.getLogger(ChunkingService.class);
    private final ChunkingPipeline chunkingPipeline;

    public ChunkingService(ChunkingPipeline chunkingPipeline) {
        this.chunkingPipeline = chunkingPipeline;
    }

    /**
     * For backward compatibility, returns raw chunk text list.
     */
    public List<String> chunkText(String text) {
        if (text == null || text.isBlank()) {
            return new ArrayList<>();
        }

        String dummyId = UUID.randomUUID().toString();
        List<Chunk> chunks = chunkDocument(text, "legacy_document", dummyId);
        List<String> textChunks = new ArrayList<>();

        for (Chunk chunk : chunks) {
            textChunks.add(chunk.getFullTextWithOverlap());
        }

        return textChunks;
    }

    /**
     * Redesigned semantic chunking entry point returning rich chunks with metadata.
     */
    public List<Chunk> chunkDocument(String text, String documentName, String documentId) {
        log.info("Starting redesigned chunking pipeline for document: {} (ID: {})", documentName, documentId);
        List<Chunk> chunks = chunkingPipeline.process(text, documentName, documentId);
        log.info("Redesigned pipeline generated {} semantic chunks.", chunks.size());

        for (int i = 0; i < chunks.size(); i++) {
            Chunk chunk = chunks.get(i);
            log.debug("""
                    Chunk {}
                    Type       : {}
                    Page       : {}
                    Hierarchy  : {}
                    Tokens     : {}
                    Preview    : {}
                    """,
                    i + 1,
                    chunk.getMetadata().getChunkType(),
                    chunk.getMetadata().getPageNumber(),
                    chunk.getMetadata().getSectionHierarchy(),
                    chunk.getMetadata().getTokenCount(),
                    chunk.getText().substring(0, Math.min(chunk.getText().length(), 100)));
        }

        return chunks;
    }
}