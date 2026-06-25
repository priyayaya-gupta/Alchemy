package com.example.alchemy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class AlchemyApplication {
    public static void main(String[] args) {
        SpringApplication.run(AlchemyApplication.class, args);
    }
}
