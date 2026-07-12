package com.example.alchemy.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
// NON_NULL: Koi bhi field null ho toh JSON response me serialize nahi hoga.
// Iska main fayda: "token" field ab JSON response me KABHI nahi aayega,
// kyunki AuthController always null pass karta hai when returning to client.
// Ye HttpOnly cookie migration ka critical security requirement hai.
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    // Login attempt ka result message — "Login successful", "User not found", etc.
    private String message;

    // JWT token — SIRF INTERNAL USE ke liye.
    // AuthService -> AuthController tak token pass karne ka zariya hai.
    // AuthController ise HttpOnly cookie me set karta hai aur phir NULL karke return karta hai.
    // JSON response me yeh field kabhi nahi aayega (@JsonInclude(NON_NULL) + controller null set karta hai).
    // JavaScript ise ACCESS nahi kar sakta — XSS attacks se protection.
    private String token;

    // Frontend pe user ka naam display karne ke liye
    private String name;

    // "ADMIN" ya "USER" — frontend isse role-based UI decide karta hai
    private String role;
}