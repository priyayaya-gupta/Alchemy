package com.example.alchemy.Controller;

import com.example.alchemy.Service.LlmService;
import com.example.alchemy.Service.RetrievalService;
import com.example.alchemy.dto.QuestionRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/query")
public class QueryController {

    private final RetrievalService retrievalService;
    private final LlmService llmService;

    public QueryController(RetrievalService retrievalService,
                           LlmService llmService) {
        this.retrievalService = retrievalService;
        this.llmService = llmService;
    }

    @PostMapping
    public String ask(@RequestBody QuestionRequest request) {

        List<String> contextList =
                retrievalService.retrieve(request.getQuestion());

        String context =
                String.join("\n", contextList);

        return llmService.generateAnswer(
                request.getQuestion(),
                context
        );
    }
}
