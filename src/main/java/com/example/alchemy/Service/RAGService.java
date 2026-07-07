package com.example.alchemy.Service;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class RAGService {

    private final RetrievalService retrievalService;
    private final LlmService llmService;
    private final CacheService cacheService;
    private final EmbeddingService embeddingService;
    private final MemoryService memoryService;
    private final ConversationService conversationService;
    private final SummaryService summaryService;


    public RAGService(RetrievalService retrievalService,
                      LlmService llmService,
                      CacheService cacheService,
                      EmbeddingService embeddingService,
                      MemoryService memoryService,
                      ConversationService conversationService,
                      SummaryService summaryService
    ) {

        this.retrievalService = retrievalService;
        this.llmService = llmService;
        this.cacheService = cacheService;
        this.embeddingService = embeddingService;
        this.memoryService = memoryService;
        this.conversationService = conversationService;
        this.summaryService = summaryService;
    }

    public String getAnswer(String sessionId,
                            String question,
                            List<String> documentIds) {

        if (question == null || question.trim().isEmpty()) {
            return "Question cannot be empty.";
        }

        conversationService.appendMessage(
                sessionId,
                "USER",
                question
        );

        List<Double> questionVector = embeddingService.embed(question);

        String cachedAnswer = cacheService.findSimilarCachedAnswer(
                questionVector,
                documentIds,
                Collections.emptyList()
        );
        if (cachedAnswer != null) {

            conversationService.appendMessage(
                    sessionId,
                    "ASSISTANT",
                    cachedAnswer
            );

            summaryService.updateSummary(sessionId);

            return cachedAnswer;
        }

        String answer;

        List<String> contextList = retrievalService.retrieve(question, documentIds);
        if (contextList == null || contextList.isEmpty()) {

            answer = "I could not find relevant content in the uploaded document.";

        } else {

            String context = String.join("\n\n", contextList);

            String summary = memoryService.getSummary(sessionId);

            if (summary == null) {
                summary = "";
            }

            answer = llmService.generateAnswer(question, context, summary);
        }

        conversationService.appendMessage(
                sessionId,
                "ASSISTANT",
                answer
        );

        if (cacheService.shouldCacheNow(
                question,
                documentIds,
                Collections.emptyList()
        )) {

            cacheService.saveSemanticCache(
                    question,
                    questionVector,
                    answer,
                    documentIds,
                    Collections.emptyList()
            );
        }

        return answer;
    }
}
