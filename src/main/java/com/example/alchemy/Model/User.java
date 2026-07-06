package com.example.alchemy.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {

    // Internal unique ID. User ko yaad nahi rakhna.
    private String userId;

    // User ka display name.
    private String name;

    // Login ke liye email use hoga.
    private String email;

    // Real password nahi store karna.
    // BCrypt hashed password store hoga.
    private String passwordHash;
}