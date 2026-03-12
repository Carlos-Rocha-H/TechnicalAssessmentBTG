package com.technicalassessment.btgpactual.controller;

import com.technicalassessment.btgpactual.dto.LoginResponse;
import com.technicalassessment.btgpactual.exception.GlobalExceptionHandler;
import com.technicalassessment.btgpactual.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void login_shouldReturn200WithToken() throws Exception {
        LoginResponse response = LoginResponse.builder()
                .token("jwt-token")
                .userId("user-001")
                .clientId("client-001")
                .role("CLIENT")
                .build();

        when(authService.login(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"cliente1\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.userId").value("user-001"))
                .andExpect(jsonPath("$.clientId").value("client-001"))
                .andExpect(jsonPath("$.role").value("CLIENT"));
    }

    @Test
    void login_shouldReturn401ForInvalidCredentials() throws Exception {
        when(authService.login(any())).thenThrow(new IllegalArgumentException("Credenciales inválidas"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"bad\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Credenciales inválidas"));
    }

    @Test
    void register_shouldReturn201WithToken() throws Exception {
        LoginResponse response = LoginResponse.builder()
                .token("register-token")
                .userId("user-new")
                .clientId("client-new")
                .role("CLIENT")
                .build();

        when(authService.register(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"nuevo\",\"password\":\"Pass1234\",\"name\":\"Juan\",\"email\":\"j@t.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("register-token"))
                .andExpect(jsonPath("$.clientId").value("client-new"))
                .andExpect(jsonPath("$.role").value("CLIENT"));
    }

    @Test
    void register_shouldReturn401ForDuplicateUsername() throws Exception {
        when(authService.register(any())).thenThrow(new IllegalArgumentException("El username 'cliente1' ya está registrado"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"cliente1\",\"password\":\"Pass1234\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("El username 'cliente1' ya está registrado"));
    }
}
