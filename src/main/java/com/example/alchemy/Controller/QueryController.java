package com.example.alchemy.Controller;

import com.example.alchemy.Service.ChatService;
import com.example.alchemy.Service.RAGService;
import com.example.alchemy.dto.QuestionRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/query")
public class QueryController {

        private final ChatService chatService;

        public QueryController(ChatService chatService) {
                this.chatService = chatService;
        }

        @PostMapping("/ask")
        public String ask(@RequestBody QuestionRequest request) {

                return chatService.chat(
                        request.getSessionId(),
                        request.getQuestion(),
                        request.getDocumentIds()
                );
        }
}