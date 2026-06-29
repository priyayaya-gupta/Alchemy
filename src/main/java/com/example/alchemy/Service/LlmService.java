package com.example.alchemy.Service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class LlmService {

    private final ChatClient chatClient;

    public LlmService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String generateAnswer(String question, String context) {
        return generateAnswer(question, context, "");
    }

    public String generateAnswer(String question, String context, String memory) {

        String prompt = """
                You are a helpful document assistant for a RAG-based PDF question answering system.

                Recent conversation memory:
                %s

                Document context:
                %s

                User question:
                %s

                Instructions:
                - Answer mainly using the document context.
                - Use recent conversation memory only to understand follow-up questions.
                - If the user says "explain again", "make it shorter", or "what about that", use memory to understand what they mean.
                - If the user asks for a summary, summarize the given context clearly.
                - If context contains partial information, still give the best possible answer from it.
                - Do not invent information not present in the document context.
                - Keep the answer simple, structured, and easy to understand.

                Answer:
                """
                .formatted(memory, context, question);

        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }
}