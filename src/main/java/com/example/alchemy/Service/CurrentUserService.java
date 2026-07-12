package com.example.alchemy.Service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    /**
     * Current authenticated user ka userId return karta hai.
     *
     * JwtAuthenticationFilter ne userId ko principal ke roop me
     * SecurityContextHolder me save kiya hota hai.
     */
    public String getCurrentUserId() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        // Agar authentication missing hai ya anonymous request hai
        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {

            throw new IllegalStateException(
                    "No authenticated user found"
            );
        }

        // JwtAuthenticationFilter ne principal me userId store kiya tha
        return authentication.getName();
    }
}