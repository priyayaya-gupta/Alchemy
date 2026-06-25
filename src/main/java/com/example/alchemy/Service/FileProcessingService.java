package com.example.alchemy.Service;

import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class FileProcessingService {

    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final QdrantService qdrantService;
    private final CacheService cacheService;

    private static final Logger log = LoggerFactory.getLogger(FileProcessingService.class);

    public FileProcessingService(
            ChunkingService chunkingService,
            EmbeddingService embeddingService,
            QdrantService qdrantService,
            CacheService cacheService) {

        this.chunkingService = chunkingService;
        this.embeddingService = embeddingService;
        this.qdrantService = qdrantService;
        this.cacheService = cacheService;
    }

    public void process(MultipartFile file) throws Exception {

        String documentId = UUID.randomUUID().toString();
        String fileName = file.getOriginalFilename();

        log.info("FILE PROCESSING STARTED");
        log.info("FILE RECEIVED: {}", fileName);

        String text = new Tika().parseToString(file.getInputStream());

        log.info("TEXT EXTRACTED");
        log.info("TEXT LENGTH: {}", text != null ? text.length() : 0);

        if (text == null || text.isEmpty()) {
            log.error("TEXT IS EMPTY AFTER TIKA PARSE");
            throw new RuntimeException("No text extracted from file");
        }

        List<String> chunks = chunkingService.chunkText(text);

        log.info("CHUNKING COMPLETED");
        log.info("TOTAL CHUNKS: {}", chunks.size());

        int id = new Random().nextInt(100000000) + 1;

        for (String chunk : chunks) {

            log.info("PROCESSING CHUNK ID: {}", id);

            List<Double> vector = embeddingService.embed(chunk);

            log.info("EMBEDDING DONE");
            log.info("VECTOR SIZE: {}", vector != null ? vector.size() : 0);

            if (vector == null || vector.isEmpty()) {
                log.error("VECTOR IS NULL OR EMPTY for chunk {}", id);
                continue;
            }

            qdrantService.store(
                    String.valueOf(id),
                    vector,
                    chunk,
                    documentId,
                    fileName);

            log.info("STORED IN QDRANT: {}", id);

            id++;
        }

        cacheService.clearRagCache();

        log.info("RAG CACHE CLEARED AFTER NEW FILE UPLOAD");
        log.info("FILE PROCESSING COMPLETED SUCCESSFULLY");
    }
}