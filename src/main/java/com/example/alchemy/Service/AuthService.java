package com.example.alchemy.Service;

import com.example.alchemy.Model.User;
import com.example.alchemy.dto.AuthResponse;
import com.example.alchemy.dto.LoginRequest;
import com.example.alchemy.dto.SignupRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.UUID;

@Service
public class AuthService {

    private final JedisPool jedisPool;
    private final BCryptPasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final JwtService jwtService;

    public AuthService(BCryptPasswordEncoder passwordEncoder,
            ObjectMapper objectMapper,
            JwtService jwtService) {

        // Redis me users store karenge.
        this.jedisPool = new JedisPool("localhost", 6379);

        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
        this.jwtService = jwtService;
    }

    public AuthResponse signup(SignupRequest request) {
        try (Jedis jedis = jedisPool.getResource()) {

            String emailKey = "user:email:" + request.getEmail();

            // Agar email already exist karta hai, duplicate signup nahi hone denge.
            if (jedis.exists(emailKey)) {
                return new AuthResponse("User already exists", null, null);
            }

            // Unique userId create hota hai.
            String userId = UUID.randomUUID().toString();

            User user = new User(
                    userId,
                    request.getName(),
                    request.getEmail(),
                    passwordEncoder.encode(request.getPassword()) // password hash
            );

            String userJson = objectMapper.writeValueAsString(user);

            // email se userId milega.
            jedis.set(emailKey, userId);

            // userId se full user data milega.
            jedis.set("user:id:" + userId, userJson);

            // Signup ke baad direct token de dete hain.
            // Isse user signup ke baad directly chatbot page ja sakta hai.
            String token = jwtService.generateToken(userId, user.getEmail());

            return new AuthResponse("Signup successful", token, user.getName());

        } catch (Exception e) {
            throw new RuntimeException("Signup failed", e);
        }
    }

    public AuthResponse login(LoginRequest request) {
        try (Jedis jedis = jedisPool.getResource()) {

            String emailKey = "user:email:" + request.getEmail();

            // Pehle email se userId nikaal rahe hain.
            String userId = jedis.get(emailKey);

            if (userId == null) {
                return new AuthResponse("User not found", null, null);
            }

            // Ab userId se full user data nikaal rahe hain.
            String userJson = jedis.get("user:id:" + userId);

            if (userJson == null) {
                return new AuthResponse("User data not found", null, null);
            }

            User user = objectMapper.readValue(userJson, User.class);

            // Raw password ko stored hash ke against match karte hain.
            boolean passwordCorrect = passwordEncoder.matches(
                    request.getPassword(),
                    user.getPasswordHash());

            if (!passwordCorrect) {
                return new AuthResponse("Invalid password", null, null);
            }

            // Login successful, ab JWT token create.
            String token = jwtService.generateToken(user.getUserId(), user.getEmail());

            return new AuthResponse("Login successful", token, user.getName());

        } catch (Exception e) {
            throw new RuntimeException("Login failed", e);
        }
    }
}