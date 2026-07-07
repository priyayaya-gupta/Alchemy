package com.example.alchemy.dto;

import org.springframework.ai.ollama.api.OllamaOptions;

public class AnswerResponse {
    private String answer;
    private boolean found;

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public boolean isFound() {
        return found;
    }

    public void setFound(boolean found) {
        this.found = found;
    }
}


