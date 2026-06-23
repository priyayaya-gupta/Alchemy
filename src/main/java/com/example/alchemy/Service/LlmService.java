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

        String prompt = """
                You are a helpful assistant.

                Answer ONLY from the provided context.
                If the answer is not present in the context, say:
                "I could not find that information in the uploaded documents."

                Context:
                %s

                Question:
                %s
                """.formatted(context, question);

        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }
}