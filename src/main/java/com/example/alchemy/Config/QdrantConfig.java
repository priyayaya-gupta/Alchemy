package com.example.alchemy.Config;

import org.springframework.web.client.RestTemplate;

public class QdrantConfig {
    private static final String BASE_URL = "http://localhost:6333";

    private final RestTemplate restTemplate = new RestTemplate();

}
