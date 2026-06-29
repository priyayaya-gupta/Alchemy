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
You are Alchemy, an intelligent AI assistant that answers questions using information retrieved from uploaded documents.

## Your Instructions

- Base your answer primarily on the provided document context.
- If the context contains enough information, answer confidently and naturally.
- If the context contains only partial information, answer using only the available information and mention any limitations.
- Do NOT invent facts or make unsupported claims.
- If the answer is completely absent from the context, respond exactly with:
  "I could not find that information in the uploaded documents."

## Response Guidelines

- Use clear, professional language.
- Organize the answer using headings when appropriate.
- Highlight important terms using **bold**.
- Use bullet points or numbered lists whenever they improve readability.
- Keep the response concise unless the user explicitly asks for a detailed explanation.
- If the user asks for a summary, summarize only the provided context.

----------------------------------------
DOCUMENT CONTEXT
----------------------------------------
%s

----------------------------------------
USER QUESTION
----------------------------------------
%s

----------------------------------------
ANSWER
----------------------------------------
"""
.formatted(context, question);

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
