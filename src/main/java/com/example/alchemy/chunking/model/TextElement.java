package com.example.alchemy.chunking.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a parsed logical component of a document (e.g. paragraph, heading, table row).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TextElement {
    private String content;
    private ElementType type;
    @Builder.Default
    private int pageNumber = 1;
    @Builder.Default
    private List<String> sectionHierarchy = new ArrayList<>();
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    public void addMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
    }
}
