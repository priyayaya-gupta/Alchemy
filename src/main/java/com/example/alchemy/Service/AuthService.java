package com.example.alchemy.Service;

import com.example.alchemy.Model.User;
import com.example.alchemy.Repository.UserRepository;
import com.example.alchemy.dto.AuthResponse;
import com.example.alchemy.dto.LoginRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    // Repository used to communicate with MySQL
    private final UserRepository userRepository;

    // BCrypt is used to compare entered password with stored hash
    private final BCryptPasswordEncoder passwordEncoder;

    // Generates JWT token after successful login
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       BCryptPasswordEncoder passwordEncoder,
                       JwtService jwtService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Login for both ADMIN and USER.
     *
     * Cookie Migration Note:
     * Yeh method AuthResponse ke andar token return karta hai — lekin yeh sirf
     * AuthController ke liye hai. Controller token ko HttpOnly cookie me set karta hai
     * aur phir JSON response me token ko NULL kar deta hai.
     *
     * Token kabhi bhi JSON HTTP response me nahi jayega:
     *   - AuthController null pass karta hai new AuthResponse() me
     *   - @JsonInclude(NON_NULL) null fields ko serialize nahi karta
     *
     * Is tarah JavaScript (aur XSS attackers) token dekh nahi sakte.
     */
    public AuthResponse login(LoginRequest request) {

        // Find user using email
        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        // Email not found — token null, name null, role null
        if (user == null) {
            return new AuthResponse(
                    "User not found",
                    null,
                    null,
                    null
            );
        }

        // Compare entered password with BCrypt hash stored in MySQL
        boolean passwordCorrect = passwordEncoder.matches(
                request.getPassword(),
                user.getPasswordHash()
        );

        // Password mismatch — token null return karo
        if (!passwordCorrect) {
            return new AuthResponse(
                    "Invalid password",
                    null,
                    null,
                    null
            );
        }

        // Successful login — JWT token generate karo.
        // Yeh token AuthController ke liye hai jo ise HttpOnly cookie me set karega.
        // JSON response me nahi jayega (AuthController null set karta hai return karte waqt).
        String token = jwtService.generateToken(
                user.getUserId(),
                user.getEmail(),
                user.getRole().name()
        );

        return new AuthResponse(
                "Login successful",
                token,              // AuthController cookie me daalta hai, JSON me nahi bhejta
                user.getName(),
                user.getRole().name()
        );
    }
}