package com.example.alchemy.Service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.stereotype.Service;

@Service
public class LlmService {

    private final ChatClient chatClient;

    public LlmService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String generateAnswer(String question, String context) {

        String prompt = """
                You are a helpful document assistant for a RAG-based PDF question answering system.

                Your job:
                - Answer using the provided document context.
                - If the user asks for a summary, summarize the given context clearly.
                - If the context contains partial information, still provide the best possible answer from it.
                - Do not say "I don't know" unless the context is empty or completely unrelated.
                - Do not use outside knowledge unless the user asks for a general explanation.
                - Keep the answer simple, structured, and easy to understand.
                -make the headlines and important details bold

                User question:
                %s

                Document context:
                %s

                Answer:
                """.formatted(question, context);

        return chatClient.prompt()
                .user(prompt)
                .options(
                        OllamaOptions.builder()
                                .temperature(0.0)
                                .build()
                )
                .call()
                .content();
    }
}