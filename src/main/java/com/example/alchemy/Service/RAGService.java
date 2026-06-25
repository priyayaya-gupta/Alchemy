package com.example.alchemy.Service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
// question leta hai pdf se answer nikalta hai aur redis cache use karke fast bana deta h

@Service
public class RAGService {
    //redis mai bucket banega query cache , redis key-->question har user ka
    @Cacheable(value = "query-cache", key = "#question")
    public String getAnswer(String question) {

        System.out.println("Cache MISS → running RAG pipeline");

        // Step 1: embedding
        // Step 2: Qdrant search
        // Step 3: LLM call

        return "Answer from PDF via RAG pipeline";
    }
    private String generateAnswer(String question) {
        // your existing logic
        return "final answer from RAG";
    }
}
