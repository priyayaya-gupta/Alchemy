package com.example.alchemy.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
@Service
public class ChunkingService {
    private static final Logger log =
            LoggerFactory.getLogger(ChunkingService.class);
    private StringBuilder getLastNSentences(String text, int n) {

        String[] sentences = text.split("\\. ");
        StringBuilder overlap = new StringBuilder();

        int start = Math.max(0, sentences.length - n);

        for (int i = start; i < sentences.length; i++) {
            overlap.append(sentences[i]).append(". ");
        }

        return overlap;
    }
    public List<String> chunkText(String text) {

        List<String> sentences = List.of(text.split("(?<=[.!?])\\s+"));

        List<String> chunks = new ArrayList<>();

        StringBuilder currentChunk = new StringBuilder();

        for (String sentence : sentences) {

            if (currentChunk.length() + sentence.length() > 1000) {

                chunks.add(currentChunk.toString().trim());

                // overlap: last 2 sentences carry forward
                currentChunk = getLastNSentences(currentChunk.toString(), 2);
            }

            currentChunk.append(sentence).append(" ");
        }

        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk.toString());
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
                    chunk
            );
        }
        return chunks;
    }

    private String getOverlap(String text, int overlapSize) {

        if (text.length() <= overlapSize) {
            return text;
        }

        return text.substring(text.length() - overlapSize);
    }
}