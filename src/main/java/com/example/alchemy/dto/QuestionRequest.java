package com.example.alchemy.dto;

import lombok.Data;

import java.util.List;

@Data
public class QuestionRequest {
    private String question;
    private List<String> documentIds;
}