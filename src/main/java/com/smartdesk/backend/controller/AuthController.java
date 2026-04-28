package com.smartdesk.backend.controller;

import com.smartdesk.backend.dto.ApiResponse;
import com.smartdesk.backend.dto.LoginRequest;
import com.smartdesk.backend.dto.RegisterRequest;
import com.smartdesk.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(
            @Valid @RequestBody LoginRequest request) {

        System.out.println("Login attempt for: " + request.getUsername());

        try {
            Map<String, Object> result = authService.login(request);
            System.out.println("Login successful for: " +
                    request.getUsername());
            return ResponseEntity.ok(ApiResponse.ok(result));

        } catch (Exception e) {
            System.err.println("Login failed for " +
                    request.getUsername() +
                    ": " + e.getMessage());
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error(
                            "Invalid username or password"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(
            @Valid @RequestBody RegisterRequest request) {

        System.out.println("Register attempt for: " + request.getUsername());

        try {
            Map<String, Object> result = authService.register(request);
            return ResponseEntity.ok(ApiResponse.ok(result));

        } catch (Exception e) {
            System.err.println("Register failed: " + e.getMessage());
            return ResponseEntity
                    .status(400)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/test")
    public ResponseEntity<ApiResponse<String>> test() {
        return ResponseEntity.ok(
                ApiResponse.ok("Auth endpoint is working", "OK"));
    }

    
}