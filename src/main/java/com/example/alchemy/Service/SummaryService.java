package com.example.alchemy.Service;

import com.example.alchemy.Model.Message;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SummaryService {

    private final LlmService llmService;
    private final ConversationService conversationService;
    private final MemoryService memoryService;

    public SummaryService(
            ConversationService conversationService,
            MemoryService memoryService,
            LlmService llmService
    ) {
        this.conversationService = conversationService;
        this.memoryService = memoryService;
        this.llmService = llmService;
    }

    public void updateSummary(String sessionId) {
        System.out.println("updateSummary called for " + sessionId);
        // Don't summarize until enough conversation exists
        boolean should = conversationService.shouldSummarize(sessionId);
        System.out.println("Should summarize = " + should);

        if (!should) {
            return;
        }

        String existingSummary = memoryService.getSummary(sessionId);

        if (existingSummary == null) {
            existingSummary = "";
        }

        List<Message> messages =
                conversationService.getMessagesToSummarize(sessionId);

        if (messages.isEmpty()) {
            return;
        }

        StringBuilder conversation = new StringBuilder();

        for (Message message : messages) {

            conversation.append(message.getRole())
                    .append(": ")
                    .append(message.getContent())
                    .append("\n");
        }

        String updatedSummary =
                llmService.generateSummary(
                        existingSummary,
                        conversation.toString());

        memoryService.saveSummary(
                sessionId,
                updatedSummary);

        System.out.println("UPDATED SUMMARY = [" + updatedSummary + "]");

        conversationService.keepRemainingMessages(sessionId);
    }
}