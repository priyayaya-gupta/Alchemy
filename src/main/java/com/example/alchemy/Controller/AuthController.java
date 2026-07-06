package com.example.alchemy.Controller;

import com.example.alchemy.Service.AuthService;
import com.example.alchemy.dto.AuthResponse;
import com.example.alchemy.dto.LoginRequest;
import com.example.alchemy.dto.SignupRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public AuthResponse signup(@RequestBody SignupRequest request) {
        // Signup request AuthService ko bhej rahe hain.
        return authService.signup(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        // Login request AuthService ko bhej rahe hain.
        return authService.login(request);
    }
}