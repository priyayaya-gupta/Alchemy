package com.example.alchemy.dto;

import lombok.Data;

@Data
public class LoginRequest {

    // User email enter karega.
    private String email;

    // User password enter karega.
    private String password;
}