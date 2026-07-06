package com.example.alchemy.Service;

import com.example.alchemy.dto.AnswerResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.stereotype.Service;


@Log4j2
@Service
public class LlmService {

    private final ChatClient chatClient;


    public LlmService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String generateAnswer(String question,
                                 String context,
                                 String conversationSummary) {

        log.info("Question: {}", question);
        log.info("Context: {}", context);
        log.info("Conversation Summary: {}", conversationSummary);

        if (conversationSummary == null) {
            conversationSummary = "";
        }

        String prompt = """
                  You are an AI assistant that answers questions using uploaded documents.
                
                  ## Rules
                
                  1. If the user is greeting you, introducing themselves, thanking you, or making casual conversation, respond naturally and politely.
                     Examples:
                     - "Hi"
                     - "Hello"
                     - "Hey"
                     - "Good morning"
                     - "Thanks"
                     - "How are you?" etc..
                 
                
                     These do NOT require document retrieval.
                
                  2. For questions about the uploaded documents, use ONLY the DOCUMENT CONTEXT.
                
                  3. Use the CONVERSATION SUMMARY only to understand follow-up questions. Never treat it as factual document content.
                
                  4. Never use your own knowledge to answer document-specific questions.
                
                  5. If the DOCUMENT CONTEXT contains enough information, answer using only that information.
                
                  6. If the DOCUMENT CONTEXT contains only partial information, begin with:
                     "Based on the uploaded documents..."
                     and answer only using the available information.
                
                  7. If the user is asking about the uploaded documents but the answer is completely absent from the DOCUMENT CONTEXT, respond exactly:
                     I could not find that information in the uploaded documents.
                  8. if no document is selected or uploaded answer with your own knowledge
                  
                ----------------------------------------
                CONVERSATION SUMMARY
                ----------------------------------------
                %s
                ----------------------------------------
                USER QUESTION
                ----------------------------------------
                %s
                
                ----------------------------------------
                ANSWER
                ----------------------------------------
                %s
                """
                .formatted(context, conversationSummary, question);


        return chatClient.prompt()
                .user(prompt)
                .options(
                        OllamaOptions.builder()
                                .temperature(0.2)
                                .topP(0.9)
                                .build()
                )
                .call()
                .content();
    }

    public String generateSummary(String existingSummary,
                                  String conversation) {

        String prompt = """
                You are a long-term conversation memory system for an AI assistant.
                
                Your task is to maintain a compact memory of the conversation using the Existing Memory and the Recent Conversation.
                
                Existing Memory:
                %s
                
                Recent Conversation:
                %s
                
                Instructions:
                
                - Merge the new conversation into the existing memory.
                - Keep important information from BOTH:
                  1. User information.
                  2. Important discussions about uploaded documents.
                
                Always keep:
                - User's name
                - User preferences
                - User goals
                - User projects
                - User skills
                - Important facts learned from uploaded documents
                - Important questions that were answered from uploaded documents
                - Key concepts discussed during the conversation
                
                Do NOT keep:
                - Greetings
                - Small talk
                - Jokes
                - Repeated information
                - Temporary confirmations like "okay", "thanks", "yes"
                
                If the same topic is discussed multiple times:
                - Merge it into a single concise memory point.
                - Avoid duplicates.
                
                Output Rules:
                - Return ONLY the updated memory.
                - Use bullet points.
                - One bullet = one fact.
                - No headings.
                - No introduction.
                - Maximum 10 bullets.
                - Keep each bullet concise.
                """.formatted(existingSummary, conversation);

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

