package com.example.alchemy.Service;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class QdrantService {

    private static final String BASE_URL = "http://localhost:6333";
    private static final String COLLECTION = "pdf_chunks";

    private final RestTemplate restTemplate = new RestTemplate();

    // ----------------------------
    // CREATE COLLECTION (SAFE)
    // ----------------------------
    public void createCollection() {

        String url = BASE_URL + "/collections/" + COLLECTION;

        Map<String, Object> request = new HashMap<>();

        request.put("vectors", Map.of(
                "size", 768,
                "distance", "Cosine"
        ));

        try {
            restTemplate.put(url, request);
            System.out.println("✅ Collection created");
        } catch (Exception e) {
            System.out.println("⚠️ Collection already exists, skipping...");
        }
    }

    // ----------------------------
    // AUTO RUN ON START
    // ----------------------------
    @PostConstruct
    public void init() {
        createCollection();
    }

    public void store(String id, List<Double> vector, String text) {

        String url = BASE_URL + "/collections/pdf_chunks/points";

        Map<String, Object> body = Map.of(
                "points", List.of(
                        Map.of(
                                "id", Integer.parseInt(id),
                                "vector", vector,
                                "payload", Map.of("text", text)
                        )
                )
        );

        restTemplate.put(url, body);
    }
}
