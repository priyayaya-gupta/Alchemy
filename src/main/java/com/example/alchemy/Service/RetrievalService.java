package com.example.alchemy.Service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RetrievalService {

    private final List<String> chunks = new ArrayList<>();

    public void saveChunks(List<String> newChunks) {
        chunks.clear();
        chunks.addAll(newChunks);
    }

    public List<String> retrieve(String question) {

        return chunks.stream()
                .filter(chunk ->
                        chunk.toLowerCase().contains(question.toLowerCase()))
                .limit(3)
                .collect(Collectors.toList());
    }
}