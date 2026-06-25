package com.example.alchemy.Service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class QdrantService {

    @Value("${qdrant.url}")
    private String baseUrl;

    @Value("${qdrant.collection}")
    private String collection;

    private final RestTemplate restTemplate = new RestTemplate();

    @PostConstruct
    public void init() {
        createCollection();
    }

    // ---------------------------
    // CREATE COLLECTION
    // ---------------------------
    public void createCollection() {

        String url = baseUrl + "/collections/" + collection;

        Map<String, Object> request = Map.of(
                "vectors", Map.of(
                        "size", 768,
                        "distance", "Cosine"
                )
        );

        try {
            restTemplate.put(url, request);
            System.out.println("Collection created");
        } catch (Exception e) {
            System.out.println("Collection already exists or creation failed: " + e.getMessage());
        }
    }

    // ---------------------------
    // STORE / UPSERT VECTORS
    // ---------------------------
     public void store(String id,
                      List<Double> vector,
                      String text,
                      String documentId,
                      String fileName){

        String url = baseUrl
                + "/collections/"
                + collection
                + "/points";

        Map<String, Object> body = Map.of(
                "points",
                List.of(
                        Map.of(
                                "id", Integer.parseInt(id),
                                "vector", vector,
                                "payload", Map.of(
                                        "text", text,
                                        "documentId", documentId,
                                        "fileName", fileName
                                )
                        )
                )
        );

        restTemplate.put(url, body);
    }

    // ---------------------------
    // SEARCH VECTORS
    // ---------------------------
    public List<String> search(List<Double> vector) {

        String url = baseUrl
                + "/collections/"
                + collection
                + "/points/search";

        Map<String, Object> body = Map.of(
                "vector", vector,
                "limit", 5,
                "with_payload", true
        );

        Map response = restTemplate.postForObject(
                url,
                body,
                Map.class
        );

        List<Map<String, Object>> result =
                (List<Map<String, Object>>) response.get("result");

        return result.stream()
                .map(point -> {
                    Map<String, Object> payload =
                            (Map<String, Object>) point.get("payload");

                    return (String) payload.get("text");
                })
                .toList();
    }
    public void deleteDocument(String documentId) {

        String url = baseUrl
                + "/collections/"
                + collection
                + "/points/delete";

        Map<String, Object> body = Map.of(
                "filter", Map.of(
                        "must", List.of(
                                Map.of(
                                        "key", "documentId",
                                        "match", Map.of(
                                                "value", documentId
                                        )
                                )
                        )
                )
        );

        restTemplate.postForObject(
                url,
                body,
                String.class
        );

        System.out.println("Deleted vectors for document: " + documentId);
    }
}



