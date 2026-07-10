package com.example.alchemy.chunking.token;

/**
 * Interface to estimate or compute the token count of a given text block.
 */
public interface TokenCounter {
    /**
     * Counts the number of tokens in the given text.
     * @param text The input string to count.
     * @return The estimated or actual token count.
     */
    int countTokens(String text);
}
