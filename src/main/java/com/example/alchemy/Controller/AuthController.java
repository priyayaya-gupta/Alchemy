package com.example.alchemy.Controller;

import com.example.alchemy.Service.AuthService;
import com.example.alchemy.dto.AuthResponse;
import com.example.alchemy.dto.LoginRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// @CrossOrigin annotation hata diya.
// CORS ab SecurityConfig me CorsConfigurationSource bean ke zariye centrally configure hai.
// Controller-level annotation ki zarurat nahi — ek jagah manage karna better hai.
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    // JWT expiration wahi value use karenge jo JwtService bhi use karta hai.
    // Cookie ka Max-Age = JWT ka expiration time (milliseconds to seconds conversion hogi).
    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // Public signup removed.
    // Ab koi bhi directly account create nahi kar sakta.
    // User/Admin creation sirf AdminController se hoga:
    // POST /api/admin/create-user

    /**
     * Login endpoint — credentials validate karta hai aur HttpOnly JWT cookie set karta hai.
     *
     * Pehle: Token JSON body me return hota tha, frontend localStorage me store karta tha.
     * Ab:    Token HttpOnly cookie me set hota hai — JavaScript access nahi kar sakta.
     *        JSON response me sirf message, name, role aata hai — token KABHI nahi.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody LoginRequest request,
            HttpServletResponse httpResponse  // HttpOnly cookie set karne ke liye inject kiya
    ) {
        // AuthService credentials validate karta hai aur token return karta hai (internal use)
        AuthResponse loginResult = authService.login(request);

        // Sirf successful login pe cookie set karo (token != null means login successful)
        if (loginResult.getToken() != null) {

            // ============================================================
            // HttpOnly JWT Cookie Setup
            // ============================================================
            Cookie jwtCookie = new Cookie("alchemyToken", loginResult.getToken());

            // HttpOnly = true:
            // JavaScript "document.cookie" ya "localStorage" se yeh cookie nahi padh sakta.
            // XSS attack me bhi attacker token steal nahi kar sakta.
            jwtCookie.setHttpOnly(true);

            // Secure = false (local development):
            // HTTP pe bhi cookie browser bhejega — development me ye zaroori hai.
            // PRODUCTION ME ZAROOR CHANGE KARO: jwtCookie.setSecure(true)
            // Secure=true hone pe cookie sirf HTTPS requests pe jayegi.
            jwtCookie.setSecure(false);

            // Path = "/" means yeh cookie puri application ke liye valid hai.
            // /api/query, /api/files, /api/admin — sab pe cookie jayegi.
            jwtCookie.setPath("/");

            // Max-Age: JWT expiration ke barabar rakhte hain (milliseconds → seconds)
            // Cookie browser me tab tak rehti hai jab tak JWT valid hai.
            jwtCookie.setMaxAge((int) (jwtExpirationMs / 1000));

            // SameSite=Lax: Baseline CSRF protection.
            // Browser cross-origin fetch requests pe automatically yeh cookie nahi bhejega.
            // Servlet 6.0 (Spring Boot 3.x) me Cookie.setAttribute() support karta hai.
            jwtCookie.setAttribute("SameSite", "Lax");

            httpResponse.addCookie(jwtCookie);
        }

        // Token ko NULL karke return karo — JSON response me token KABHI nahi aayega.
        // @JsonInclude(NON_NULL) in AuthResponse ensure karta hai ki null fields serialize nahi hongi.
        // Frontend ko milega: { "message": "Login successful", "name": "...", "role": "..." }
        // Token JSON me NAHI hoga — browser cookie me safe store ho gaya hai.
        return ResponseEntity.ok(new AuthResponse(
                loginResult.getMessage(),
                null,                    // Token JSON me nahi — cookie me ja chuka hai
                loginResult.getName(),
                loginResult.getRole()
        ));
    }

    /**
     * Logout endpoint — browser ka JWT cookie expire karke clear karta hai.
     *
     * Kaise kaam karta hai:
     * Same naam ki cookie dobara set karte hain, lekin value empty aur Max-Age=0.
     * Browser Max-Age=0 dekhta hai aur cookie turant delete kar deta hai.
     *
     * Frontend isko call karega: fetch("/api/auth/logout", { method: "POST", credentials: "include" })
     * SecurityConfig me /api/auth/logout permitAll() hai — unauthenticated user bhi logout kar sake.
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletResponse httpResponse) {

        // Same naam, same path — lekin value empty aur Max-Age = 0
        Cookie expiredCookie = new Cookie("alchemyToken", "");
        expiredCookie.setHttpOnly(true);
        expiredCookie.setSecure(false); // Local dev ke liye (production me true)
        expiredCookie.setPath("/");
        expiredCookie.setMaxAge(0);     // 0 = browser is cookie ko turant delete kar dega
        expiredCookie.setAttribute("SameSite", "Lax");

        httpResponse.addCookie(expiredCookie);

        return ResponseEntity.ok("Logged out successfully");
    }
}