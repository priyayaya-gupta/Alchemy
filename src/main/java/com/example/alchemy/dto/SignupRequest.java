package com.example.alchemy.dto;

import lombok.Data;

@Data
public class SignupRequest {

    // Signup page se name aayega.
    private String name;

    // Signup/login ke liye email.
    private String email;

    // Raw password frontend se aayega.
    // Isko directly store nahi karenge.
    private String password;
}