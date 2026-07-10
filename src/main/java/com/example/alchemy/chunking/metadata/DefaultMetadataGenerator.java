package com.example.alchemy.chunking.metadata;

import com.example.alchemy.chunking.model.Chunk;
import com.example.alchemy.chunking.model.ChunkMetadata;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Enriches chunks with document identifiers and positional index attributes.
 */
@Component
public class DefaultMetadataGenerator implements MetadataGenerator {

    @Override
    public List<Chunk> generateMetadata(List<Chunk> chunks, String documentName, String documentId) {
        if (chunks == null || chunks.isEmpty()) {
            return new ArrayList<>();
        }

        for (int i = 0; i < chunks.size(); i++) {
            Chunk chunk = chunks.get(i);
            ChunkMetadata metadata = chunk.getMetadata();

            if (metadata == null) {
                metadata = new ChunkMetadata();
                chunk.setMetadata(metadata);
            }

            metadata.setDocumentName(documentName);
            metadata.setDocumentId(documentId);
            metadata.setChunkIndex(i);
        }

        return chunks;
    }
}
