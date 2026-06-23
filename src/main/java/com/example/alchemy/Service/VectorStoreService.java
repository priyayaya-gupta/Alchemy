package com.example.alchemy.Service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class VectorStoreService {

    private final RestTemplate restTemplate = new RestTemplate();

    private final String QDRANT_URL = "http://localhost:6333";
    private final String COLLECTION = "rag_collection";

    public void store(String id, List<Double> vector, String text, String fileName) {

        Map<String, Object> payload = new HashMap<>();
        payload.put("text", text);
        payload.put("fileName", fileName);

        Map<String, Object> point = new HashMap<>();
        point.put("id", id);
        point.put("vector", vector);
        point.put("payload", payload);

        Map<String, Object> body = Map.of("points", List.of(point));

        restTemplate.postForObject(
                QDRANT_URL + "/collections/" + COLLECTION + "/points",
                body,
                String.class
        );
    }

    public List<String> search(List<Double> queryVector) {

        Map<String, Object> body = new HashMap<>();
        body.put("vector", queryVector);
        body.put("top", 5);
        body.put("with_payload", true);

        Map res = restTemplate.postForObject(
                QDRANT_URL + "/collections/" + COLLECTION + "/points/search",
                body,
                Map.class
        );

        List<Map> result = (List<Map>) res.get("result");

        List<String> chunks = new ArrayList<>();

        for (Map r : result) {
            Map payload = (Map) r.get("payload");
            chunks.add((String) payload.get("text"));
        }

        return chunks;
    }
}
