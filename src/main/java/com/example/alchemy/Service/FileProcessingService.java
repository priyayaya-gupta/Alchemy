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

    private static final Logger log =
            LoggerFactory.getLogger(FileProcessingService.class);

    public FileProcessingService(
            ChunkingService chunkingService,
            EmbeddingService embeddingService,
            QdrantService qdrantService) {

        this.chunkingService = chunkingService;
        this.embeddingService = embeddingService;
        this.qdrantService = qdrantService;
    }

    public void process(MultipartFile file) throws Exception {
        String documentId = UUID.randomUUID().toString();
        String fileName = file.getOriginalFilename();
        // 1. File received
        log.info("FILE PROCESSING STARTED");
        log.info(" FILE RECEIVED: {}", file.getOriginalFilename());

        // 2. Text extraction
        String text = new Tika().parseToString(file.getInputStream());

        log.info("TEXT EXTRACTED");
        log.info(" TEXT LENGTH: {}", text != null ? text.length() : 0);

        if (text == null || text.isEmpty()) {
            log.error("TEXT IS EMPTY AFTER TIKA PARSE");
            throw new RuntimeException("No text extracted from file");
        }

        // 3. Chunking
        List<String> chunks = chunkingService.chunkText(text);

        log.info(" CHUNKING COMPLETED");
        log.info(" TOTAL CHUNKS: {}", chunks.size());

        AtomicInteger chunkCounter = new AtomicInteger(1);

        // 4. Embedding + Qdrant storage
        for (String chunk : chunks) {

            log.info("PROCESSING CHUNK ID: {}", id);

            // embedding
            List<Double> vector = embeddingService.embed(chunk);

            log.info("EMBEDDING DONE");
            log.info("VECTOR SIZE: {}", vector != null ? vector.size() : 0);

            if (vector == null || vector.isEmpty()) {
                log.error("VECTOR IS NULL OR EMPTY for chunk {}", id);
                continue;
            }

            // store in Qdrant
            qdrantService.store(
                    String.valueOf(id),
                    vector,
                    chunk
            );

            log.info("STORED IN QDRANT: {}", id);

            id++;
        }

        log.info(" FILE PROCESSING COMPLETED SUCCESSFULLY");
    }
}