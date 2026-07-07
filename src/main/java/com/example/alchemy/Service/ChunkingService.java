package com.example.alchemy.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChunkingService {

    private static final Logger log = LoggerFactory.getLogger(ChunkingService.class);

    private static final int MAX_CHUNK_SIZE = 1200;
    private static final int OVERLAP_SENTENCES = 1;

    public List<String> chunkText(String text) {

        List<String> chunks = new ArrayList<>();

        if (text == null || text.isBlank()) {
            return chunks;
        }

        text = cleanText(text);

        List<String> sections = splitByHeadings(text);

        for (String section : sections) {
            chunks.addAll(splitLargeSection(section));
        }

        log.info("Generated {} chunks.", chunks.size());

        for (int i = 0; i < chunks.size(); i++) {

            String chunk = chunks.get(i);

            log.info("""
                    Chunk {}
                    Characters : {}
                    Words      : {}
                    Content    : {}
                    """,
                    i + 1,
                    chunk.length(),
                    chunk.split("\\s+").length,
                    chunk);
        }

        return chunks;
    }

    private String cleanText(String text) {
        return text
                .replace("\r", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .replaceAll("[ \\t]+", " ")
                .trim();
    }

    private List<String> splitByHeadings(String text) {

        List<String> sections = new ArrayList<>();

        String[] lines = text.split("\\n");

        String currentHeading = "";
        StringBuilder currentSection = new StringBuilder();

        for (String line : lines) {

            String trimmed = line.trim();

            if (trimmed.isEmpty()) {
                continue;
            }

            if (isHeading(trimmed)) {

                if (currentSection.length() > 0) {
                    sections.add(currentSection.toString().trim());
                    currentSection = new StringBuilder();
                }

                currentHeading = trimmed;
                currentSection.append(currentHeading).append("\n");

            } else {
                currentSection.append(trimmed).append(" ");
            }
        }

        if (currentSection.length() > 0) {
            sections.add(currentSection.toString().trim());
        }

        if (sections.isEmpty()) {
            sections.add(text);
        }

        return sections;
    }

    private boolean isHeading(String line) {

        if (line.length() > 100) {
            return false;
        }

        boolean numberedHeading = line.matches("^(\\d+\\.?|\\d+\\.\\d+\\.?|[A-Z]\\.|[IVX]+\\.)\\s+.*");

        boolean allCapsHeading = line.equals(line.toUpperCase()) && line.length() > 4;

        boolean titleLikeHeading = line.matches("^[A-Z][A-Za-z0-9\\s,:;()\\-/]{3,90}$")
                && !line.endsWith(".")
                && !line.endsWith("?")
                && !line.endsWith("!");

        return numberedHeading || allCapsHeading || titleLikeHeading;
    }

    private List<String> splitLargeSection(String section) {

        List<String> chunks = new ArrayList<>();

        if (section.length() <= MAX_CHUNK_SIZE) {
            chunks.add(section);
            return chunks;
        }

        List<String> sentences = splitIntoSentences(section);

        String heading = sentences.isEmpty() ? "" : sentences.get(0);
        StringBuilder currentChunk = new StringBuilder();

        List<String> recentSentences = new ArrayList<>();

        for (String sentence : sentences) {

            if (currentChunk.length() + sentence.length() > MAX_CHUNK_SIZE
                    && currentChunk.length() > 0) {

                chunks.add(currentChunk.toString().trim());

                currentChunk = new StringBuilder();

                if (!heading.isBlank()) {
                    currentChunk.append(heading).append("\n");
                }

                int start = Math.max(0, recentSentences.size() - OVERLAP_SENTENCES);

                for (int i = start; i < recentSentences.size(); i++) {
                    currentChunk.append(recentSentences.get(i)).append(" ");
                }
            }

            currentChunk.append(sentence).append(" ");
            recentSentences.add(sentence);
        }

        if (!currentChunk.toString().trim().isEmpty()) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }

    private List<String> splitIntoSentences(String text) {

        List<String> sentences = new ArrayList<>();

        String[] parts = text.split("(?<=[.!?])\\s+");

        for (String part : parts) {
            String trimmed = part.trim();

            if (!trimmed.isEmpty()) {
                sentences.add(trimmed);
            }
        }

        return sentences;
    }
}