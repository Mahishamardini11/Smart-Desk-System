package com.smartdesk.backend.util;


import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashGenerator {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

        String rawPassword = "password";
        String hash = encoder.encode(rawPassword);

        System.out.println("Raw password : " + rawPassword);
        System.out.println("BCrypt hash  : " + hash);
        System.out.println("Verify test  : " +
                encoder.matches(rawPassword, hash));

        // Also verify the hash we use in SQL
        String sqlHash =
                "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
        System.out.println("SQL hash OK  : " +
                encoder.matches(rawPassword, sqlHash));
    }
}