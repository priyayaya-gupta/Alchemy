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

        @PostMapping
        public String ask(@RequestBody QuestionRequest request) {

                return ragService.getAnswer(
                        request.getQuestion(),
                        request.getDocumentIds()
                );
        }
}