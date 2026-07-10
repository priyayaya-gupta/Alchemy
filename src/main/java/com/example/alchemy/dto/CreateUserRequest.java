package com.example.alchemy.dto;

import com.example.alchemy.Model.Role;
import lombok.Data;

@Data
public class CreateUserRequest {

    // Admin jis user/admin ko create karega uska name
    private String name;

    // Login email
    private String email;

    // Plain password request me aayega
    // DB me hash ban ke save hoga
    private String password;

    // USER ya ADMIN
    private Role role;
}