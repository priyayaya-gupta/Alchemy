package com.example.alchemy.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // ================================================================
                // CSRF: Abhi ke liye disable rakha hai.
                //
                // Reason: Frontend abhi purane localStorage-based flow par hai.
                // Agar CSRF enable karein toh frontend ke saare POST requests (login, query,
                // upload, create-user) fail ho jaenge — kyunki frontend CSRF token nahi bhej raha.
                //
                // Protection jo abhi bhi hai:
                // - Cookie par SameSite=Lax set hai — cross-origin fetch requests pe
                //   browser automatically cookie nahi bhejta (Lax mode me top-level navigation ke alawa).
                // - Ye baseline CSRF protection hai.
                //
                // TODO — Frontend Migration Complete Hone Par:
                // Inhe un-comment karo aur upar wali csrf.disable() line hata do:
                //
                // import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
                // .csrf(csrf -> csrf
                //     .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                //     // Login endpoint ko CSRF se exempt karo (pehli request — cookie nahi hogi)
                //     .ignoringRequestMatchers("/api/auth/login")
                // )
                //
                // Aur frontend me har state-changing request pe X-XSRF-TOKEN header add karna hoga.
                // ================================================================
                .csrf(csrf -> csrf.disable())

                // ================================================================
                // CORS: Cookie-based requests ke liye credentials support zaroori hai.
                //
                // Pehle: Har controller par @CrossOrigin tha — wildcard origins, no credentials.
                // Ab:    Ek jagah centrally configure — specific origins, allowCredentials=true.
                //
                // allowCredentials=true: fetch(..., { credentials: "include" }) ke saath
                // browser cookie automatically attach karega cross-origin requests pe bhi.
                //
                // NOTE: Spring Security me CORS configuration filter chain me apply hoti hai,
                // isliye controller-level @CrossOrigin se pehle yeh effective hoti hai.
                // ================================================================
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // ================================================================
                // Session Management: STATELESS — server pe koi HTTP session nahi banegi.
                // JWT cookie hi authentication handle karta hai.
                // Har request self-contained hai — cookie me token hota hai.
                // ================================================================
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth

                        // Frontend static pages: Public — koi authentication nahi chahiye
                        // Page load hoga, phir JavaScript cookie se authenticated status check karega
                        .requestMatchers(
                                "/",
                                "/login.html",
                                "/index.html",
                                "/create-user.html",
                                "/css/**",
                                "/js/**",
                                "/favicon.ico"
                        ).permitAll()

                        // Login: Public — yahan par cookie nahi hoti abhi tak
                        .requestMatchers("/api/auth/login").permitAll()

                        // Logout: Public — stale ya expired cookie bhi clear ho sake.
                        // Unauthenticated user bhi logout kar sake (edge case handling).
                        .requestMatchers("/api/auth/logout").permitAll()

                        // Admin APIs: Sirf ADMIN role ke liye — JwtAuthenticationFilter
                        // role set karta hai, SecurityConfig yahan enforce karta hai
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // Protected APIs: Valid JWT cookie/header required
                        .requestMatchers(
                                "/api/query",
                                "/api/files",
                                "/api/files/**"
                        ).authenticated()

                        .anyRequest().authenticated()
                )

                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                // JWT Filter: UsernamePasswordAuthenticationFilter se pehle run karega.
                // Cookie se token padh kar SecurityContextHolder set karta hai.
                // Authorization header fallback bhi is filter me hai (Postman testing).
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    /**
     * CORS Configuration Bean
     *
     * Browser ke fetch(..., { credentials: "include" }) requests ke liye yeh zaroori hai.
     * "credentials: include" tab use hota hai jab cookie automatically attach karni ho
     * (jo HttpOnly cookie migration me zaroori hai).
     *
     * IMPORTANT: allowCredentials(true) ke saath allowedOrigins me "*" use nahi kar sakte.
     * Specific origins likhne zaroori hain.
     *
     * Local Development Ports:
     * - Spring Boot default: 8080
     * - Agar alag port use kar rahe ho toh yahan add karo
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Allowed origins — production me apna domain add karo
        // Local development ke liye Spring Boot default ports
        config.setAllowedOrigins(List.of(
                "http://localhost:8080",
                "http://127.0.0.1:8080"
        ));

        // Allowed HTTP methods
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Allowed headers — Content-Type aur Authorization (Postman fallback ke liye)
        // X-XSRF-TOKEN: Future CSRF implementation ke liye already include kar diya
        config.setAllowedHeaders(List.of("Content-Type", "Authorization", "X-XSRF-TOKEN"));

        // allowCredentials = true: Cookie-based requests ke liye mandatory.
        // Iske bina browser HttpOnly cookie cross-origin requests pe attach nahi karega.
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}