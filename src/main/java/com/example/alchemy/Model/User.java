package com.example.alchemy.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity // Is class ki MySQL table banegi
@Table(name = "users") // Table name users hoga
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    // MySQL automatically id generate karega
    private Long id;

    @Column(unique = true, nullable = false)
    // userId bhi unique rakhenge
    private String userId;

    private String name;

    @Column(unique = true, nullable = false)
    // Same email se duplicate user nahi banega
    private String email;

    @Column(nullable = false)
    // Plain password nahi, BCrypt hash store hoga
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    // MySQL me ADMIN ya USER text form me save hoga
    private Role role;
}