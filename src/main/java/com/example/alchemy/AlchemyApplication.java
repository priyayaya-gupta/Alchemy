package com.example.alchemy;

import com.example.alchemy.Model.Role;
import com.example.alchemy.Model.User;
import com.example.alchemy.Repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.UUID;

@SpringBootApplication
@EnableCaching
public class AlchemyApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlchemyApplication.class, args);
    }

    /**
     * This method runs automatically every time the application starts.
     *
     * Purpose:
     * Create the first ADMIN only if it doesn't already exist.
     */
    @Bean
    CommandLineRunner createDefaultAdmin(UserRepository userRepository,
                                         BCryptPasswordEncoder passwordEncoder) {

        return args -> {

            // Check if admin already exists using email
            if (userRepository.findByEmail("admin@alchemy.com").isPresent()) {

                System.out.println("Default Admin already exists.");

                return;
            }

            // Create new Admin
            User admin = new User();

            // Public unique id used inside JWT
            admin.setUserId(UUID.randomUUID().toString());

            // Display name
            admin.setName("System Administrator");

            // Login email
            admin.setEmail("admin@alchemy.com");

            // Store BCrypt hash instead of plain password
            admin.setPasswordHash(
                    passwordEncoder.encode("admin123")
            );

            // Give ADMIN privileges
            admin.setRole(Role.ADMIN);

            // Save into MySQL
            userRepository.save(admin);

            System.out.println("========================================");
            System.out.println(" Default Admin Created Successfully");
            System.out.println(" Email    : admin@alchemy.com");
            System.out.println(" Password : admin123");
            System.out.println("========================================");
        };
    }
}