package com.example.alchemy.Service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

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

    public List<Map<String, String>> listDocuments() {
        String url = baseUrl + "/collections/" + collection + "/points/scroll";
        Map<String, Object> body = Map.of(
                "limit", 1000,
                "with_payload", true,
                "with_vector", false
        );

        try {
            Map response = restTemplate.postForObject(url, body, Map.class);
            if (response == null || !response.containsKey("result")) {
                return List.of();
            }
            Map<String, Object> result = (Map<String, Object>) response.get("result");
            List<Map<String, Object>> points = (List<Map<String, Object>>) result.get("points");
            if (points == null) {
                return List.of();
            }

            Map<String, String> uniqueDocs = new HashMap<>();
            for (Map<String, Object> point : points) {
                Map<String, Object> payload = (Map<String, Object>) point.get("payload");
                if (payload != null && payload.containsKey("documentId") && payload.containsKey("fileName")) {
                    String docId = (String) payload.get("documentId");
                    String fileName = (String) payload.get("fileName");
                    uniqueDocs.put(docId, fileName);
                }
            }

            List<Map<String, String>> list = new ArrayList<>();
            for (Map.Entry<String, String> entry : uniqueDocs.entrySet()) {
                list.add(Map.of("documentId", entry.getKey(), "fileName", entry.getValue()));
            }
            list.sort(Comparator.comparing(m -> m.get("fileName")));
            return list;
        } catch (Exception e) {
            System.err.println("Error listing documents from Qdrant: " + e.getMessage());
            return List.of();
        }
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



