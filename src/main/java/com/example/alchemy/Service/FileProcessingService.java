package com.example.alchemy.Service;

import com.example.alchemy.chunking.model.Chunk;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

@Service
public class FileProcessingService {

    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final QdrantService qdrantService;
    private final CacheService cacheService;

    private static final Logger log = LoggerFactory.getLogger(FileProcessingService.class);

    public FileProcessingService(ChunkingService chunkingService,
            EmbeddingService embeddingService,
            QdrantService qdrantService,
            CacheService cacheService) {

        this.chunkingService = chunkingService;
        this.embeddingService = embeddingService;
        this.qdrantService = qdrantService;
        this.cacheService = cacheService;
    }

    public String process(MultipartFile file) throws Exception {

        String documentId = UUID.randomUUID().toString();
        String fileName = file.getOriginalFilename();

        log.info("FILE PROCESSING STARTED");
        log.info("FILE RECEIVED: {}", fileName);

        String text = new Tika().parseToString(file.getInputStream());

        log.info("TEXT EXTRACTED");
        log.info("TEXT LENGTH: {}", text != null ? text.length() : 0);

        if (text == null || text.isBlank()) {
            throw new RuntimeException("No text extracted from file");
        }

        List<Chunk> chunks = chunkingService.chunkDocument(text, fileName, documentId);

        log.info("CHUNKING COMPLETED");
        log.info("TOTAL CHUNKS: {}", chunks.size());

        for (Chunk chunk : chunks) {
            int chunkIndex = chunk.getMetadata().getChunkIndex();
            String chunkText = chunk.getFullTextWithOverlap();

            log.info("PROCESSING CHUNK INDEX: {}", chunkIndex);
            log.info("CHUNK PREVIEW: {}", chunkText.substring(0, Math.min(chunkText.length(), 150)));

            List<Double> vector = embeddingService.embed(chunkText);

            log.info("EMBEDDING DONE");
            log.info("VECTOR SIZE: {}", vector != null ? vector.size() : 0);

            if (vector == null || vector.isEmpty()) {
                log.error("VECTOR IS NULL OR EMPTY for chunk index {}", chunkIndex);
                continue;
            }

            String pointId = createPointId(documentId, chunkIndex);

            qdrantService.store(
                    pointId,
                    vector,
                    chunkText,
                    chunk.getMetadata().toMap());

            log.info("STORED IN QDRANT. POINT ID: {}", pointId);
        }

        cacheService.clearRagCache();

        log.info("RAG CACHE CLEARED AFTER NEW FILE UPLOAD");
        log.info("FILE PROCESSING COMPLETED SUCCESSFULLY");

        return documentId;
    }

    private String createPointId(String documentId, int chunkIndex) {
        return String.valueOf(Math.abs((documentId + "-" + chunkIndex).hashCode()));
    }
}