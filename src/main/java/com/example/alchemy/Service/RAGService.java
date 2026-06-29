package com.example.alchemy.Service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RAGService {

    private final RetrievalService retrievalService;
    private final LlmService llmService;
    private final CacheService cacheService;
    private final EmbeddingService embeddingService;

    public RAGService(RetrievalService retrievalService,
            LlmService llmService,
            CacheService cacheService,
            EmbeddingService embeddingService) {
        this.retrievalService = retrievalService;
        this.llmService = llmService;
        this.cacheService = cacheService;
        this.embeddingService = embeddingService;
    }

    public String getAnswer(String question,List<String> documentIds) {

        if (question == null || question.trim().isEmpty()) {
            return "Question cannot be empty.";
        }

        List<Double> questionVector = embeddingService.embed(question);

        String cachedAnswer = cacheService.findSimilarCachedAnswer(questionVector);

        if (cachedAnswer != null) {
            return cachedAnswer;
        }

        String retrievalQuery = buildRetrievalQuery(question);

        List<String> contextList = retrievalService.retrieve(retrievalQuery, documentIds
                );
        if (contextList == null || contextList.isEmpty()) {
            return "I could not find relevant content in the uploaded document.";
        }

        String context = String.join("\n\n", contextList);

        String answer = llmService.generateAnswer(question, context);

        if (cacheService.shouldCacheNow(question)) {
            cacheService.saveSemanticCache(question, questionVector, answer);
        } else {
            System.out.println("Answer not cached yet. Waiting for repeated question.");
        }

        return answer;
    }

    private String buildRetrievalQuery(String question) {

        String q = question.toLowerCase();

        if (q.contains("summarize") ||
                q.contains("summarise") ||
                q.contains("summary") ||
                q.contains("overview") ||
                q.contains("main points") ||
                q.contains("key points")) {

            return "main topic key points overview important information conclusion";
        }

        return question;
    }
}