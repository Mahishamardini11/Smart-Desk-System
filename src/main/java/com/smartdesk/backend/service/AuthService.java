package com.smartdesk.backend.service;

import com.smartdesk.backend.dto.LoginRequest;
import com.smartdesk.backend.dto.RegisterRequest;
import com.smartdesk.backend.entity.User;
import com.smartdesk.backend.repository.UserRepository;
import com.smartdesk.backend.security.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       AuthenticationManager authenticationManager,
                       UserDetailsService userDetailsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
    }

    public Map<String, Object> login(LoginRequest request) {
        System.out.println("AuthService.login called for: " +
                request.getUsername());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername().trim(),
                            request.getPassword()
                    )
            );
            System.out.println("Authentication successful: " +
                    authentication.isAuthenticated());

        } catch (BadCredentialsException e) {
            System.err.println("Bad credentials for: " +
                    request.getUsername());
            throw new RuntimeException("Invalid username or password");
        } catch (Exception e) {
            System.err.println("Auth error: " + e.getClass().getName() +
                    " - " + e.getMessage());
            throw new RuntimeException("Authentication failed: " +
                    e.getMessage());
        }

        UserDetails userDetails = userDetailsService
                .loadUserByUsername(request.getUsername().trim());

        String token = jwtUtil.generateToken(userDetails);

        User user = userRepository
                .findByUsername(request.getUsername().trim())
                .orElseThrow(() ->
                        new RuntimeException("User not found after auth"));

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("username", user.getUsername());
        response.put("email", user.getEmail());
        response.put("role", user.getRole());
        response.put("userId", user.getId());

        System.out.println("Login response built for: " +
                user.getUsername());
        return response;
    }

    public Map<String, Object> register(RegisterRequest request) {
        System.out.println("AuthService.register called for: " +
                request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setEmail(request.getEmail().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("USER");

        User saved = userRepository.save(user);
        System.out.println("User registered: " + saved.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "User registered successfully");
        response.put("username", saved.getUsername());
        return response;
    }

    public boolean verifyPassword(String raw, String encoded) {
        return passwordEncoder.matches(raw, encoded);
    }
}