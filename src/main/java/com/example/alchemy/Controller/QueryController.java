package com.example.alchemy.Controller;

import com.example.alchemy.Service.RAGService;
import com.example.alchemy.dto.QuestionRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

// @CrossOrigin annotation hata diya.
// CORS ab SecurityConfig me CorsConfigurationSource bean ke zariye centrally manage hai.
@RestController
@RequestMapping("/api")
public class QueryController {

    private final RAGService ragService;

    // JwtService dependency REMOVE kar di.
    //
    // Pehle: Yeh controller khud Authorization header se token nikalta tha,
    //        manually validate karta tha, aur userId extract karta tha.
    //        Yeh duplicate kaam tha kyunki JwtAuthenticationFilter pehle hi yeh sab karta hai.
    //
    // Ab:    JwtAuthenticationFilter (jo pehle run karta hai) cookie/header se token padh leta hai,
    //        validate kar leta hai, aur userId ko SecurityContextHolder me principal ke roop me set kar deta hai.
    //        Controller sirf SecurityContextHolder se userId le leta hai — no manual token handling.

    public QueryController(RAGService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/query")
    public String askQuestion(@RequestBody QuestionRequest request) {

        // ================================================================
        // Authentication already ho chuki hai — JwtAuthenticationFilter ne ki hai.
        //
        // Filter ne:
        //   1. Cookie / Authorization header se token padha
        //   2. JwtService se validate kiya
        //   3. userId ko principal ke roop me SecurityContextHolder me set kiya
        //
        // Hume sirf SecurityContextHolder se principal (userId) nikalna hai.
        // Manually token parse karne ki zarurat nahi — filter ne kaam kar diya hai.
        // ================================================================
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Extra safety check — SecurityConfig me /api/query .authenticated() hai,
        // toh normally Spring Security pehle hi 403 return kar deta agar auth nahi hoti.
        // "anonymousUser" Spring Security ka default principal hota hai unauthenticated requests ke liye.
        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return "Unauthorized. Please login first.";
        }

        // JwtAuthenticationFilter ne userId (user ka UUID) ko principal ke roop me set kiya tha
        // (JwtAuthenticationFilter me: new UsernamePasswordAuthenticationToken(userId, null, authorities))
        String userId = (String) authentication.getPrincipal();

        // RAG flow same rahega — bas userId ab SecurityContextHolder se aya hai,
        // Authorization header ya cookie se manually nahi
        return ragService.answerQuestion(
                request.getQuestion(),
                request.getDocumentIds(),
                request.getFileNames(),
                userId
        );
    }
}