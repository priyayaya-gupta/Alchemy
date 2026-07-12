package com.example.alchemy.Service;

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

    // SecurityContextHolder se current logged-in user ka userId deta hai
    private final CurrentUserService currentUserService;

    public ChatService(
            EmbeddingService embeddingService,
            CacheService cacheService,
            RetrievalService retrievalService,
            LlmService llmService,
            ConversationService conversationService,
            MemoryService memoryService,
            SummaryService summaryService,
            CurrentUserService currentUserService
    ) {
        this.embeddingService = embeddingService;
        this.cacheService = cacheService;
        this.retrievalService = retrievalService;
        this.llmService = llmService;
        this.conversationService = conversationService;
        this.memoryService = memoryService;
        this.summaryService = summaryService;
        this.currentUserService = currentUserService;
    }

    public String chat(
            String sessionId,
            String question,
            List<String> documentIds
    ) {

        /*
         * JwtAuthenticationFilter ne authenticated user's userId
         * SecurityContextHolder me principal ke roop me save kiya hai.
         */
        String userId = currentUserService.getCurrentUserId();

        /*
         * Agar frontend sessionId na bheje, "default" use hoga.
         *
         * Random UUID use nahi kar rahe, kyunki har request par naya UUID
         * banne se conversation history aur summary continue nahi hoti.
         */
        String safeSessionId =
                sessionId == null || sessionId.isBlank()
                        ? "default"
                        : sessionId.trim();

        /*
         * userId aur sessionId combine karne se different users ki
         * conversation memory mix nahi hogi.
         *
         * Example:
         * User A -> abc123:default
         * User B -> xyz789:default
         */
        String userSessionId = userId + ":" + safeSessionId;

        /*
         * documentIds null ho sakta hai.
         * Empty list use karne se NullPointerException avoid hoga.
         */
        List<String> safeDocumentIds =
                documentIds == null
                        ? Collections.emptyList()
                        : documentIds;

        // Current user's message conversation me store karna
        conversationService.appendMessage(
                userSessionId,
                "user",
                question
        );

        // Current user-session ki existing summary load karna
        String summary = memoryService.getSummary(userSessionId);

        if (summary == null) {
            summary = "";
        }

        // Question ka embedding generate karna
        List<Double> vector = embeddingService.embed(question);

        // Existing semantic cache me similar answer search karna
        String cachedAnswer = cacheService.findSimilarCachedAnswer(
                vector,
                safeDocumentIds,
                Collections.emptyList()
        );

        if (cachedAnswer != null) {

            // Cached answer ko bhi current user's conversation me add karna
            conversationService.appendMessage(
                    userSessionId,
                    "assistant",
                    cachedAnswer
            );

            // Zarurat hone par current user-session ki summary update karna
            summaryService.updateSummary(userSessionId);

            return cachedAnswer;
        }

        // Selected documents se relevant chunks retrieve karna
        List<String> chunks = retrievalService.retrieve(
                question,
                safeDocumentIds
        );

        // Retrieved chunks ko LLM context me combine karna
        String context = String.join("\n\n", chunks);

        // Context aur conversation summary use karke answer generate karna
        String answer = llmService.generateAnswer(
                question,
                context,
                summary
        );

        // Generated answer current user's conversation me save karna
        conversationService.appendMessage(
                userSessionId,
                "assistant",
                answer
        );

        // Conversation enough badi ho toh summary update hogi
        summaryService.updateSummary(userSessionId);

        // Cache admission policy check karna
        boolean shouldCache = cacheService.shouldCacheNow(
                question,
                safeDocumentIds,
                Collections.emptyList()
        );

        if (shouldCache) {

            // Eligible answer ko semantic cache me save karna
            cacheService.saveSemanticCache(
                    question,
                    vector,
                    answer,
                    safeDocumentIds,
                    Collections.emptyList()
            );
        }

        return answer;
    }
}