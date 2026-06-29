package com.example.alchemy.Service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RAGService {

    private final RetrievalService retrievalService;
    private final LlmService llmService;
    private final CacheService cacheService;
    private final EmbeddingService embeddingService;
    private final MemoryService memoryService;

    public RAGService(RetrievalService retrievalService,
            LlmService llmService,
            CacheService cacheService,
            EmbeddingService embeddingService,
            MemoryService memoryService) {
        this.retrievalService = retrievalService;
        this.llmService = llmService;
        this.cacheService = cacheService;
        this.embeddingService = embeddingService;
        this.memoryService = memoryService;
    }

    public String getAnswer(String question,
            List<String> documentIds,
            List<String> fileNames,
            String sessionId) {

        if (question == null || question.trim().isEmpty()) {
            return "Question cannot be empty.";
        }

        List<Double> questionVector = embeddingService.embed(question);

        String cachedAnswer = cacheService.findSimilarCachedAnswer(
                questionVector,
                documentIds,
                fileNames);

        if (cachedAnswer != null) {
            memoryService.saveTurn(sessionId, question, cachedAnswer);
            return cachedAnswer;
        }

        String retrievalQuery = buildRetrievalQuery(question);

        List<String> contextList = retrievalService.retrieve(
                retrievalQuery,
                documentIds,
                fileNames);

        if (contextList == null || contextList.isEmpty()) {
            return "I could not find relevant content in the uploaded document.";
        }

        String context = String.join("\n\n", contextList);
        String memory = String.join("\n\n", memoryService.getRecentMemory(sessionId));

        String answer = llmService.generateAnswer(question, context, memory);

        memoryService.saveTurn(sessionId, question, answer);

        if (cacheService.shouldCacheNow(question, documentIds, fileNames)) {
            cacheService.saveSemanticCache(
                    question,
                    questionVector,
                    answer,
                    documentIds,
                    fileNames);
        }

        return answer;
    }

    private String buildRetrievalQuery(String question) {
        String q = question.toLowerCase();

        if (q.contains("disqualification") ||
                q.contains("disqualified") ||
                q.contains("not eligible") ||
                q.contains("eligibility criteria")) {

            return question + " disqualification criteria not eligible rejection conditions";
        }

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