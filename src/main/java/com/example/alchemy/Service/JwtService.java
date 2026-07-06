package com.example.alchemy.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    private SecretKey getSigningKey() {
        // Secret key JWT sign/verify karne ke liye use hoti hai.
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateToken(String userId, String email) {

        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtExpirationMs);

        // JWT ke andar userId and email store kar rahe hain.
        // Password kabhi JWT me nahi daalna.
        return Jwts.builder()
                .subject(userId)
                .claim("email", email)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUserId(String token) {
        // Token ke subject me userId rakha hai.
        return getClaims(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = getClaims(token);

            // Token expired nahi hona chahiye.
            return claims.getExpiration().after(new Date());

        } catch (Exception e) {
            // Invalid/expired token yahan fail hoga.
            return false;
        }
    }

    private Claims getClaims(String token) {
        // Token verify karke claims nikalta hai.
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}