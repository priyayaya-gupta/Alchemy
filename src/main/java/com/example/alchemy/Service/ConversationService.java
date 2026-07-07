package com.example.alchemy.Service;

import com.example.alchemy.Model.Message;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConversationService {

    // sessionId -> conversation
    private final Map<String, List<Message>> conversations =
            new ConcurrentHashMap<>();

    /**
     * Add a message to the conversation.
     */
    public void appendMessage(
            String sessionId,
            String role,
            String content
    ) {

        conversations
                .computeIfAbsent(
                        sessionId,
                        id -> new ArrayList<>())
                .add(new Message(role, content));
    }

    /**
     * Returns the current conversation.
     */
    public List<Message> getConversation(String sessionId) {

        return List.copyOf(
                conversations.getOrDefault(
                        sessionId,
                        List.of()));
    }

    /**
     * Number of messages in conversation.
     */
    public int getMessageCount(String sessionId) {

        return conversations
                .getOrDefault(
                        sessionId,
                        List.of())
                .size();
    }

    /**
     * Decide whether the conversation should be summarized.
     */
    public boolean shouldSummarize(String sessionId) {

        return getMessageCount(sessionId) >= 10;
    }

    /**
     * Clear conversation after summary is generated.
     */


    public List<Message> getMessagesToSummarize(String sessionId) {

        List<Message> messages =
                conversations.getOrDefault(sessionId, List.of());

        if (messages.size() < 10) {
            return List.of();
        }

        return new ArrayList<>(
                messages.subList(0, 8));
    }
    public void keepRemainingMessages(String sessionId) {

        List<Message> messages = conversations.get(sessionId);

        if (messages == null || messages.size() < 10) {
            return;
        }

        List<Message> remaining =
                new ArrayList<>(
                        messages.subList(8, messages.size()));

        conversations.put(sessionId, remaining);
    }
}