package com.example.alchemy.chunking.token;

import org.springframework.stereotype.Component;

/**
 * An implementation of TokenCounter that estimates tokens based on word count.
 * A typical rule of thumb is that 1 word in English is roughly 1.3 to 1.4 tokens.
 */
@Component
public class WordCountTokenCounter implements TokenCounter {
    private static final double WORDS_TO_TOKENS_RATIO = 1.3;

    @Override
    public int countTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        // Split by whitespace
        String[] words = text.trim().split("\\s+");
        return (int) Math.ceil(words.length * WORDS_TO_TOKENS_RATIO);
    }
}
