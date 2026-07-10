package com.example.alchemy.Repository;

import com.example.alchemy.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Repository is the bridge between Spring Boot and MySQL.
// We don't write SQL manually for basic CRUD operations.
public interface UserRepository extends JpaRepository<User, Long> {

    // Find user by email.
    // Used during login and while checking duplicate users.
    Optional<User> findByEmail(String email);

    // Find user using our custom userId.
    Optional<User> findByUserId(String userId);
}