package com.example.alchemy.Controller;

import com.example.alchemy.Model.Role;
import com.example.alchemy.Model.User;
import com.example.alchemy.Repository.UserRepository;
import com.example.alchemy.dto.CreateUserRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin
public class AdminController {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AdminController(UserRepository userRepository,
                           BCryptPasswordEncoder passwordEncoder) {

        // MySQL users table se communicate karne ke liye
        this.userRepository = userRepository;

        // Plain password ko BCrypt hash me convert karne ke liye
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/create-user")
    public ResponseEntity<String> createUser(
            @RequestBody CreateUserRequest request) {

        // Same email se duplicate account create nahi hone denge
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity
                    .badRequest()
                    .body("User already exists");
        }

        // Naya User object create kar rahe hain
        User user = new User();

        // Har user ko unique UUID milega
        user.setUserId(UUID.randomUUID().toString());

        // Request se basic user details set kar rahe hain
        user.setName(request.getName());
        user.setEmail(request.getEmail());

        // Plain password kabhi MySQL me save nahi hoga
        // BCrypt hash save hoga
        user.setPasswordHash(
                passwordEncoder.encode(request.getPassword())
        );

        // Agar role request me nahi aaya, default USER banega
        if (request.getRole() == null) {
            user.setRole(Role.USER);
        } else {
            // Admin USER ya ADMIN dono create kar sakta hai
            user.setRole(request.getRole());
        }

        // User ko MySQL users table me save kar rahe hain
        userRepository.save(user);

        // HTTP 200 response
        return ResponseEntity.ok("Account created successfully");
    }
}