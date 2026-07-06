package com.example.alchemy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {

    // Message like "Login successful"
    private String message;

    // JWT token frontend localStorage me save karega.
    private String token;

    // Optional: frontend display ke liye name.
    private String name;
}
