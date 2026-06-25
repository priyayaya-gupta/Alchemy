package com.example.alchemy.Service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RetrievalService {

    private final EmbeddingService embeddingService;
    private final QdrantService qdrantService;

    public RetrievalService(
            EmbeddingService embeddingService,
            QdrantService qdrantService) {

        this.embeddingService = embeddingService;
        this.qdrantService = qdrantService;
    }

    public List<String> retrieve(String question) {

        List<Double> queryVector =
                embeddingService.embed(question);

        return qdrantService.search(queryVector);
    }
}