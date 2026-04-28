package com.smartdesk.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdesk.backend.controller.AuthController;
import com.smartdesk.backend.dto.LoginRequest;
import com.smartdesk.backend.dto.RegisterRequest;
import com.smartdesk.backend.security.JwtFilter;
import com.smartdesk.backend.security.UserDetailsServiceImpl;
import com.smartdesk.backend.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtFilter jwtFilter;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void login_withValidCredentials_returns200() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("password");

        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("token", "mock-jwt-token");
        mockResult.put("username", "admin");
        mockResult.put("role", "ADMIN");

        when(authService.login(any(LoginRequest.class)))
                .thenReturn(mockResult);

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("mock-jwt-token"));
    }

    @Test
    void login_withInvalidCredentials_returns400() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("bad");
        request.setPassword("wrong");

        when(authService.login(any()))
                .thenThrow(new RuntimeException("Bad credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void register_withValidData_returns200() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@example.com");
        request.setPassword("password123");

        Map<String, Object> mockResult = new HashMap<>();
        mockResult.put("message", "User registered successfully");

        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(mockResult);

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}