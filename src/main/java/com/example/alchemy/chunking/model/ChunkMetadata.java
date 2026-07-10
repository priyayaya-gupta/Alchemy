package com.example.alchemy.chunking.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Rich metadata attributes for a document chunk.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChunkMetadata {
    private String documentName;
    private String documentId;
    private int pageNumber;
    private String sectionHierarchy;
    private int chunkIndex;
    private String chunkType;
    private int tokenCount;

    @Builder.Default
    private Map<String, Object> extraMetadata = new HashMap<>();

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("fileName", documentName);
        map.put("documentId", documentId);
        map.put("pageNumber", pageNumber);
        map.put("sectionHierarchy", sectionHierarchy != null ? sectionHierarchy : "");
        map.put("chunkIndex", chunkIndex);
        map.put("chunkType", chunkType);
        map.put("tokenCount", tokenCount);
        if (extraMetadata != null) {
            map.putAll(extraMetadata);
        }
        return map;
    }
}
