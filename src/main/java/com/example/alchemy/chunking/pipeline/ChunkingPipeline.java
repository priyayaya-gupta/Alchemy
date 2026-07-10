package com.example.alchemy.chunking.pipeline;

import com.example.alchemy.chunking.cleaner.DocumentCleaner;
import com.example.alchemy.chunking.config.ChunkingConfig;
import com.example.alchemy.chunking.metadata.MetadataGenerator;
import com.example.alchemy.chunking.model.Chunk;
import com.example.alchemy.chunking.model.TextElement;
import com.example.alchemy.chunking.overlap.OverlapProcessor;
import com.example.alchemy.chunking.parser.DocumentParser;
import com.example.alchemy.chunking.semantic.SemanticChunker;
import com.example.alchemy.chunking.structure.StructureDetector;
import com.example.alchemy.chunking.token.TokenCounter;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Orchestrator pipeline that processes raw text through consecutive stages:
 * Parser -> Cleaner -> Structure Detector -> Semantic Chunker -> Overlap Processor -> Metadata Generator.
 */
@Component
public class ChunkingPipeline {

    private final DocumentParser parser;
    private final DocumentCleaner cleaner;
    private final StructureDetector structureDetector;
    private final SemanticChunker semanticChunker;
    private final OverlapProcessor overlapProcessor;
    private final MetadataGenerator metadataGenerator;
    private final TokenCounter tokenCounter;
    private final ChunkingConfig config;

    public ChunkingPipeline(
            DocumentParser parser,
            DocumentCleaner cleaner,
            StructureDetector structureDetector,
            SemanticChunker semanticChunker,
            OverlapProcessor overlapProcessor,
            MetadataGenerator metadataGenerator,
            TokenCounter tokenCounter,
            ChunkingConfig config) {
        this.parser = parser;
        this.cleaner = cleaner;
        this.structureDetector = structureDetector;
        this.semanticChunker = semanticChunker;
        this.overlapProcessor = overlapProcessor;
        this.metadataGenerator = metadataGenerator;
        this.tokenCounter = tokenCounter;
        this.config = config;
    }

    /**
     * Executes the chunking pipeline.
     * @param rawText The raw text document.
     * @param documentName The document file name.
     * @param documentId The unique document identifier.
     * @return The list of generated rich chunks.
     */
    public List<Chunk> process(String rawText, String documentName, String documentId) {
        // 1. Parser
        List<TextElement> parsed = parser.parse(rawText);

        // 2. Cleaner
        List<TextElement> cleaned = cleaner.clean(parsed);

        // 3. Structure Detector
        List<TextElement> structured = structureDetector.detectStructure(cleaned);

        // 4. Semantic Chunker
        List<Chunk> chunked = semanticChunker.chunk(structured, config, tokenCounter);

        // 5. Overlap Processor
        List<Chunk> withOverlap = overlapProcessor.processOverlap(chunked, config);

        // 6. Metadata Generator
        return metadataGenerator.generateMetadata(withOverlap, documentName, documentId);
    }
}
