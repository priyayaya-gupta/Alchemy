package com.example.alchemy.Service;

import com.example.alchemy.Service.*;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class ChatService {

    private final EmbeddingService embeddingService;
    private final CacheService cacheService;
    private final RetrievalService retrievalService;
    private final LlmService llmService;
    private final ConversationService conversationService;
    private final MemoryService memoryService;
    private final SummaryService summaryService;

    public ChatService(
            EmbeddingService embeddingService,
            CacheService cacheService,
            RetrievalService retrievalService,
            LlmService llmService,
            ConversationService conversationService,
            MemoryService memoryService,
            SummaryService summaryService
    ) {
        this.embeddingService = embeddingService;
        this.cacheService = cacheService;
        this.retrievalService = retrievalService;
        this.llmService = llmService;
        this.conversationService = conversationService;
        this.memoryService = memoryService;
        this.summaryService = summaryService;
    }

    public String chat(
            String sessionId,
            String question,
            List<String> documentIds
    ) {

        // Save user message
        conversationService.appendMessage(
                sessionId,
                "user",
                question
        );

        // Load conversation summary
        String summary =
                memoryService.getSummary(sessionId);

        if (summary == null) {
            summary = "";
        }

        // Generate embedding
        List<Double> vector =
                embeddingService.embed(question);

        // Semantic cache lookup
        String cached = cacheService.findSimilarCachedAnswer(
                vector,
                documentIds,
                Collections.emptyList()
        );
        if (cached != null) {

            conversationService.appendMessage(
                    sessionId,
                    "assistant",
                    cached
            );

            summaryService.updateSummary(sessionId);

            return cached;
        }

        // Retrieve chunks
        List<String> chunks =
                retrievalService.retrieve(
                        question,
                        documentIds
                );

        String context =
                String.join("\n\n", chunks);

        // LLM Answer
        String answer =
                llmService.generateAnswer(
                        question,
                        context,
                        summary
                );

        // Save assistant response
        conversationService.appendMessage(
                sessionId,
                "assistant",
                answer
        );

        // Update conversation summary if needed
        summaryService.updateSummary(sessionId);

        // Cache answer if admission policy allows
        if (cacheService.shouldCacheNow(
                question,
                documentIds,
                Collections.emptyList()
        )) {

            cacheService.saveSemanticCache(
                    question,
                    vector,
                    answer,
                    documentIds,
                    Collections.emptyList()
            );
        }

        return answer;
    }
}