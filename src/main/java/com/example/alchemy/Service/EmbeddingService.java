package com.example.alchemy.Service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class EmbeddingService {

    private final RestTemplate restTemplate = new RestTemplate();

    public List<Double> embed(String text) {

        String url = "http://localhost:11434/api/embeddings";

        Map<String, Object> req = new HashMap<>();
        req.put("model", "nomic-embed-text");
        req.put("prompt", text);

        Map res = restTemplate.postForObject(url, req, Map.class);

        return (List<Double>) res.get("embedding");
    }
}
