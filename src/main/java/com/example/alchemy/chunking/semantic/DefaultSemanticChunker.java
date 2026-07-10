package com.example.alchemy.chunking.semantic;

import com.example.alchemy.chunking.config.ChunkingConfig;
import com.example.alchemy.chunking.model.Chunk;
import com.example.alchemy.chunking.model.ChunkMetadata;
import com.example.alchemy.chunking.model.ElementType;
import com.example.alchemy.chunking.model.TextElement;
import com.example.alchemy.chunking.token.TokenCounter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Concrete implementation of SemanticChunker designed to split on logical boundaries
 * and preserve document structures.
 */
@Component
public class DefaultSemanticChunker implements SemanticChunker {

    @Override
    public List<Chunk> chunk(List<TextElement> elements, ChunkingConfig config, TokenCounter tokenCounter) {
        List<Chunk> chunks = new ArrayList<>();
        if (elements == null || elements.isEmpty()) {
            return chunks;
        }

        StringBuilder currentChunkText = new StringBuilder();
        List<TextElement> currentChunkElements = new ArrayList<>();
        List<String> currentHierarchy = new ArrayList<>();
        int currentPage = -1;

        for (TextElement el : elements) {
            if (currentPage == -1) {
                currentPage = el.getPageNumber();
            }

            // Heading is a hard boundary: yield current block first
            if (el.getType() == ElementType.HEADING) {
                if (currentChunkText.length() > 0) {
                    chunks.add(buildChunk(currentChunkText.toString(), currentChunkElements, tokenCounter));
                    currentChunkText = new StringBuilder();
                    currentChunkElements = new ArrayList<>();
                }
                currentChunkText.append(el.getContent()).append("\n");
                currentChunkElements.add(el);
                currentHierarchy = el.getSectionHierarchy();
                currentPage = el.getPageNumber();
                continue;
            }

            // Indivisible structural blocks: TABLE, CODE_BLOCK, FAQ
            if (el.getType() == ElementType.TABLE || el.getType() == ElementType.CODE_BLOCK || el.getType() == ElementType.FAQ) {
                if (currentChunkText.length() > 0) {
                    chunks.add(buildChunk(currentChunkText.toString(), currentChunkElements, tokenCounter));
                    currentChunkText = new StringBuilder();
                    currentChunkElements = new ArrayList<>();
                }

                int blockTokens = tokenCounter.countTokens(el.getContent());
                if (blockTokens <= config.getMaxTokenSize()) {
                    // Fits as a single chunk
                    chunks.add(buildChunk(el.getContent(), List.of(el), tokenCounter));
                } else {
                    // Break block up gracefully without losing row context or formatting
                    splitLargeStructuralBlock(el, chunks, config, tokenCounter);
                }
                currentPage = el.getPageNumber();
                continue;
            }

            // Normal paragraphs or list items
            int elTokens = tokenCounter.countTokens(el.getContent());

            // If the element itself is too large, split it by sentence boundaries
            if (elTokens > config.getMaxTokenSize()) {
                if (currentChunkText.length() > 0) {
                    chunks.add(buildChunk(currentChunkText.toString(), currentChunkElements, tokenCounter));
                    currentChunkText = new StringBuilder();
                    currentChunkElements = new ArrayList<>();
                }
                splitParagraphBySentences(el, chunks, config, tokenCounter);
                currentPage = el.getPageNumber();
                continue;
            }

            // Determine if adding this element would exceed max limits, or if section/page changed
            int currentTokens = tokenCounter.countTokens(currentChunkText.toString());
            boolean hierarchyChanged = !el.getSectionHierarchy().equals(currentHierarchy);
            boolean pageChanged = el.getPageNumber() != currentPage;

            if (currentTokens + elTokens > config.getMaxTokenSize() || 
                    (hierarchyChanged && currentTokens >= config.getMinTokenSize()) ||
                    (pageChanged && currentTokens > 0)) {
                // Yield current chunk
                chunks.add(buildChunk(currentChunkText.toString(), currentChunkElements, tokenCounter));
                currentChunkText = new StringBuilder();
                currentChunkElements = new ArrayList<>();
            }

            if (currentChunkText.length() > 0) {
                currentChunkText.append("\n");
            }
            currentChunkText.append(el.getContent());
            currentChunkElements.add(el);
            currentHierarchy = el.getSectionHierarchy();
            currentPage = el.getPageNumber();
        }

        // Add remaining content
        if (currentChunkText.length() > 0) {
            chunks.add(buildChunk(currentChunkText.toString(), currentChunkElements, tokenCounter));
        }

        return chunks;
    }

    private void splitParagraphBySentences(TextElement el, List<Chunk> chunks, ChunkingConfig config, TokenCounter tokenCounter) {
        String content = el.getContent();
        // Split by standard sentence terminators (. ! ?) followed by spaces
        String[] sentences = content.split("(?<=[.!?])\\s+");

        StringBuilder currentBlockText = new StringBuilder();
        List<TextElement> sentenceElements = new ArrayList<>();

        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            int sentTokens = tokenCounter.countTokens(trimmed);
            int blockTokens = tokenCounter.countTokens(currentBlockText.toString());

            if (blockTokens + sentTokens > config.getTargetTokenSize() && blockTokens > 0) {
                chunks.add(buildChunk(currentBlockText.toString(), sentenceElements, tokenCounter, el.getPageNumber(), el.getSectionHierarchy(), el.getType()));
                currentBlockText = new StringBuilder();
                sentenceElements = new ArrayList<>();
            }

            if (currentBlockText.length() > 0) {
                currentBlockText.append(" ");
            }
            currentBlockText.append(trimmed);
            sentenceElements.add(TextElement.builder()
                    .content(trimmed)
                    .type(el.getType())
                    .pageNumber(el.getPageNumber())
                    .sectionHierarchy(el.getSectionHierarchy())
                    .build());
        }

        if (currentBlockText.length() > 0) {
            chunks.add(buildChunk(currentBlockText.toString(), sentenceElements, tokenCounter, el.getPageNumber(), el.getSectionHierarchy(), el.getType()));
        }
    }

    private void splitLargeStructuralBlock(TextElement el, List<Chunk> chunks, ChunkingConfig config, TokenCounter tokenCounter) {
        String content = el.getContent();

        if (el.getType() == ElementType.CODE_BLOCK) {
            // Split code block by lines
            String[] lines = content.split("\\n");
            StringBuilder currentBlockText = new StringBuilder();

            // Detect language header e.g. ```java
            String langHeader = "```";
            if (lines.length > 0 && lines[0].startsWith("```")) {
                langHeader = lines[0];
            }

            for (String line : lines) {
                if (line.trim().startsWith("```")) {
                    continue;
                }

                int lineTokens = tokenCounter.countTokens(line);
                int codeTokens = tokenCounter.countTokens(currentBlockText.toString());

                if (codeTokens + lineTokens > config.getTargetTokenSize() && codeTokens > 0) {
                    // Close current code sub-block and yield
                    String finalCodeBlock = langHeader + "\n" + currentBlockText.toString().trim() + "\n```";
                    chunks.add(buildChunk(finalCodeBlock, List.of(el), tokenCounter, el.getPageNumber(), el.getSectionHierarchy(), ElementType.CODE_BLOCK));
                    currentBlockText = new StringBuilder();
                }

                currentBlockText.append(line).append("\n");
            }

            if (currentBlockText.length() > 0) {
                String finalCodeBlock = langHeader + "\n" + currentBlockText.toString().trim() + "\n```";
                chunks.add(buildChunk(finalCodeBlock, List.of(el), tokenCounter, el.getPageNumber(), el.getSectionHierarchy(), ElementType.CODE_BLOCK));
            }
        } 
        else if (el.getType() == ElementType.TABLE) {
            // Split table by rows
            String[] lines = content.split("\\n");
            if (lines.length <= 2) {
                // Not enough markdown context, yield raw
                chunks.add(buildChunk(content, List.of(el), tokenCounter, el.getPageNumber(), el.getSectionHierarchy(), ElementType.TABLE));
                return;
            }

            // Assume first 2 lines are table headers and column dividers
            String header = lines[0] + "\n" + lines[1];
            StringBuilder currentBlockText = new StringBuilder();

            for (int r = 2; r < lines.length; r++) {
                String row = lines[r];
                int rowTokens = tokenCounter.countTokens(row);
                int tableTokens = tokenCounter.countTokens(currentBlockText.toString());

                if (tableTokens + rowTokens > config.getTargetTokenSize() && tableTokens > 0) {
                    // Prepend headers to sub-table chunk
                    String subTable = header + "\n" + currentBlockText.toString().trim();
                    chunks.add(buildChunk(subTable, List.of(el), tokenCounter, el.getPageNumber(), el.getSectionHierarchy(), ElementType.TABLE));
                    currentBlockText = new StringBuilder();
                }

                currentBlockText.append(row).append("\n");
            }

            if (currentBlockText.length() > 0) {
                String subTable = header + "\n" + currentBlockText.toString().trim();
                chunks.add(buildChunk(subTable, List.of(el), tokenCounter, el.getPageNumber(), el.getSectionHierarchy(), ElementType.TABLE));
            }
        } 
        else {
            // FAQ or other types that overflow: split by line breaks
            String[] lines = content.split("\\n");
            StringBuilder currentBlockText = new StringBuilder();

            for (String line : lines) {
                int lineTokens = tokenCounter.countTokens(line);
                int blockTokens = tokenCounter.countTokens(currentBlockText.toString());

                if (blockTokens + lineTokens > config.getTargetTokenSize() && blockTokens > 0) {
                    chunks.add(buildChunk(currentBlockText.toString().trim(), List.of(el), tokenCounter, el.getPageNumber(), el.getSectionHierarchy(), el.getType()));
                    currentBlockText = new StringBuilder();
                }
                currentBlockText.append(line).append("\n");
            }

            if (currentBlockText.length() > 0) {
                chunks.add(buildChunk(currentBlockText.toString().trim(), List.of(el), tokenCounter, el.getPageNumber(), el.getSectionHierarchy(), el.getType()));
            }
        }
    }

    private Chunk buildChunk(String text, List<TextElement> sourceElements, TokenCounter tokenCounter) {
        int page = 1;
        List<String> hierarchy = new ArrayList<>();
        ElementType type = ElementType.UNKNOWN;

        if (sourceElements != null && !sourceElements.isEmpty()) {
            page = sourceElements.get(0).getPageNumber();
            hierarchy = sourceElements.get(0).getSectionHierarchy();
            type = sourceElements.get(0).getType();
        }

        return buildChunk(text, sourceElements, tokenCounter, page, hierarchy, type);
    }

    private Chunk buildChunk(String text, List<TextElement> sourceElements, TokenCounter tokenCounter,
                             int page, List<String> hierarchy, ElementType type) {
        int tokens = tokenCounter.countTokens(text);
        String sectionPath = String.join(" > ", hierarchy);

        ChunkMetadata metadata = ChunkMetadata.builder()
                .pageNumber(page)
                .sectionHierarchy(sectionPath)
                .chunkType(type.name())
                .tokenCount(tokens)
                .build();

        return Chunk.builder()
                .text(text)
                .metadata(metadata)
                .build();
    }
}
