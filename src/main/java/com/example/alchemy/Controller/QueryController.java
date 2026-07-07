package com.example.alchemy.Controller;

import com.example.alchemy.Service.RAGService;
import com.example.alchemy.dto.QuestionRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/query")
public class QueryController {

        private final RAGService ragService;

        public QueryController(RAGService ragService) {
                this.ragService = ragService;
        }

        @PostMapping("/ask")
        public String ask(@RequestBody QuestionRequest request) {

                return ragService.getAnswer(
                        request.getSessionId(),
                        request.getQuestion(),
                        request.getDocumentIds()
                );
        }
}