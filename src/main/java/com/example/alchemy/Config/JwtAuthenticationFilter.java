package com.example.alchemy.Config;

import com.example.alchemy.Service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = null;

        // ================================================================
        // STEP 1: HttpOnly Cookie se token nikaalte hain (Primary method)
        //
        // Browser automatically "alchemyToken" cookie har request ke saath
        // same-origin calls me attach karta hai — frontend ko kuch nahi karna.
        // JavaScript yeh cookie read nahi kar sakta (HttpOnly=true).
        // ================================================================
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("alchemyToken".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break; // Cookie mil gayi, loop band karo
                }
            }
        }

        // ================================================================
        // STEP 2: Authorization Bearer header fallback (Postman/API testing ke liye)
        //
        // Agar cookie me token nahi mila toh Authorization header check karo.
        // Browser frontend always cookie use karega — yeh sirf manual API testing ke liye hai.
        // Format: Authorization: Bearer <jwt_token>
        // ================================================================
        if (token == null) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7); // "Bearer " (7 chars) ke baad actual token
            }
        }

        // ================================================================
        // STEP 3: Koi token nahi mila — unauthenticated request
        //
        // Public endpoints (/login.html, /api/auth/login, static files) ke liye
        // yeh normal flow hai — Security filter chain handle karega.
        // Protected endpoints 403 Forbidden return karenge SecurityConfig ke rules se.
        // ================================================================
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // ================================================================
        // STEP 4: Token validate karo aur SecurityContext set karo
        //
        // Valid token: userId aur role extract karke Spring Security context me daalo.
        // Invalid/expired token: SecurityContext empty rahega → protected endpoints 403 karenge.
        // ================================================================
        if (jwtService.isTokenValid(token)) {

            String userId = jwtService.extractUserId(token);
            String role = jwtService.extractRole(token);

            // Spring Security role ko "ROLE_ADMIN" / "ROLE_USER" format me expect karta hai
            SimpleGrantedAuthority authority =
                    new SimpleGrantedAuthority("ROLE_" + role);

            // Principal = userId (QueryController aur doosre controllers isse SecurityContextHolder se nikalenge)
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,  // Principal: user ka UUID — SecurityContextHolder me available hoga
                            null,    // Credentials: JWT based auth me password nahi chahiye
                            List.of(authority)
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        // Token invalid/expired: SecurityContext me koi authentication set nahi ki
        // SecurityConfig ke rules protected endpoints ke liye 403 return karenge

        filterChain.doFilter(request, response);
    }
}