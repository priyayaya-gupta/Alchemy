package com.example.alchemy.chunking.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for the redesigned chunking system.
 */
@Component
@ConfigurationProperties(prefix = "app.chunking")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChunkingConfig {
    @Builder.Default
    private int targetTokenSize = 350;

    @Builder.Default
    private int minTokenSize = 150;

    @Builder.Default
    private int maxTokenSize = 500;

    @Builder.Default
    private int overlapSentences = 2;

    @Builder.Default
    private double paragraphMergeSimilarity = 0.6;

    @Builder.Default
    private List<String> ocrNoisePatterns = new ArrayList<>(List.of(
            "^\\s*[~=_!@$%^&*]{3,}.*", // Starts with 3 or more noise symbols
            "[\\p{Punct}&&[^.\\-:,?!\"'()|`#]]{4,}", // Repeated stray punctuation like ~~~~ or ====
            "[A-Za-z0-9]{25,}", // Strings that are too long to be English words
            "^[\\s\\p{Punct}]+$", // Lines containing only symbols/whitespace
            "\\b(?:[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,4})\\b"
    ));
}
