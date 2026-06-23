package com.example.alchemy.Service;

import org.springframework.stereotype.Service;

import java.util.*;
@Service
public class ChunkingService {

    public List<String> chunkText(String text) {

        List<String> sentences = List.of(text.split("(?<=[.!?])\\s+"));

        List<String> chunks = new ArrayList<>();

        StringBuilder currentChunk = new StringBuilder();

        for (String sentence : sentences) {

            if (currentChunk.length() + sentence.length() > 1000) {

                chunks.add(currentChunk.toString());

                // overlap: last 20 chars carry forward
                String overlap = getOverlap(currentChunk.toString(), 20);
                currentChunk = new StringBuilder(overlap);
            }

            currentChunk.append(sentence).append(" ");
        }

        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk.toString());
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