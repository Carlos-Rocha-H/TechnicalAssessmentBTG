package com.technicalassessment.btgpactual.service;

import com.technicalassessment.btgpactual.dto.LoginRequest;
import com.technicalassessment.btgpactual.dto.LoginResponse;
import com.technicalassessment.btgpactual.dto.RegisterRequest;
import com.technicalassessment.btgpactual.model.Client;
import com.technicalassessment.btgpactual.model.User;
import com.technicalassessment.btgpactual.repository.ClientRepository;
import com.technicalassessment.btgpactual.repository.UserRepository;
import com.technicalassessment.btgpactual.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_shouldReturnTokenForValidCredentials() {
        User user = User.builder()
                .userId("user-001")
                .username("cliente1")
                .passwordHash("$2a$10$hashed")
                .role("CLIENT")
                .clientId("client-001")
                .build();

        when(userRepository.findByUsername("cliente1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "$2a$10$hashed")).thenReturn(true);
        when(jwtUtil.generateToken("user-001", "client-001", "CLIENT")).thenReturn("token-jwt");

        LoginResponse response = authService.login(
                LoginRequest.builder().username("cliente1").password("password123").build());

        assertEquals("token-jwt", response.getToken());
        assertEquals("user-001", response.getUserId());
        assertEquals("client-001", response.getClientId());
        assertEquals("CLIENT", response.getRole());
    }

    @Test
    void login_shouldThrowForInvalidUsername() {
        when(userRepository.findByUsername("noexiste")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.login(
                        LoginRequest.builder().username("noexiste").password("pass").build()));

        assertEquals("Credenciales inválidas", ex.getMessage());
    }

    @Test
    void login_shouldThrowForInvalidPassword() {
        User user = User.builder()
                .userId("user-001")
                .username("cliente1")
                .passwordHash("$2a$10$hashed")
                .role("CLIENT")
                .clientId("client-001")
                .build();

        when(userRepository.findByUsername("cliente1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.login(
                        LoginRequest.builder().username("cliente1").password("wrong").build()));

        assertEquals("Credenciales inválidas", ex.getMessage());
    }

    @Test
    void login_shouldReturnAdminToken() {
        User admin = User.builder()
                .userId("admin-001")
                .username("admin")
                .passwordHash("$2a$10$adminHash")
                .role("ADMIN")
                .clientId(null)
                .build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("admin123", "$2a$10$adminHash")).thenReturn(true);
        when(jwtUtil.generateToken("admin-001", null, "ADMIN")).thenReturn("admin-token");

        LoginResponse response = authService.login(
                LoginRequest.builder().username("admin").password("admin123").build());

        assertEquals("admin-token", response.getToken());
        assertEquals("ADMIN", response.getRole());
        assertNull(response.getClientId());
    }

    @Test
    void register_shouldCreateClientAndUserAndReturnToken() {
        RegisterRequest request = RegisterRequest.builder()
                .username("nuevo_user")
                .password("Pass1234")
                .name("Juan Nuevo")
                .email("juan@test.com")
                .phone("3001234567")
                .notificationPreference("SMS")
                .build();

        when(userRepository.findByUsername("nuevo_user")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Pass1234")).thenReturn("$2a$10$encoded");
        when(jwtUtil.generateToken(anyString(), anyString(), eq("CLIENT"))).thenReturn("register-token");

        LoginResponse response = authService.register(request);

        assertEquals("register-token", response.getToken());
        assertEquals("CLIENT", response.getRole());
        assertNotNull(response.getClientId());
        assertNotNull(response.getUserId());

        ArgumentCaptor<Client> clientCaptor = ArgumentCaptor.forClass(Client.class);
        verify(clientRepository).save(clientCaptor.capture());
        Client savedClient = clientCaptor.getValue();
        assertEquals("Juan Nuevo", savedClient.getName());
        assertEquals("juan@test.com", savedClient.getEmail());
        assertEquals("SMS", savedClient.getNotificationPreference());
        assertEquals(500000.0, savedClient.getBalance());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("nuevo_user", savedUser.getUsername());
        assertEquals("$2a$10$encoded", savedUser.getPasswordHash());
        assertEquals("CLIENT", savedUser.getRole());
    }

    @Test
    void register_shouldThrowWhenUsernameAlreadyExists() {
        RegisterRequest request = RegisterRequest.builder()
                .username("cliente1")
                .password("Pass1234")
                .build();

        when(userRepository.findByUsername("cliente1")).thenReturn(Optional.of(User.builder().build()));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.register(request));
        assertTrue(ex.getMessage().contains("ya está registrado"));
    }

    @Test
    void register_shouldThrowWhenUsernameMissing() {
        RegisterRequest request = RegisterRequest.builder()
                .password("Pass1234")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.register(request));
        assertEquals("Username y password son obligatorios", ex.getMessage());
    }

    @Test
    void register_shouldUseEmailAsDefaultNotification() {
        RegisterRequest request = RegisterRequest.builder()
                .username("user_default")
                .password("Pass1234")
                .name("Default User")
                .email("default@test.com")
                .build();

        when(userRepository.findByUsername("user_default")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Pass1234")).thenReturn("$2a$10$encoded");
        when(jwtUtil.generateToken(anyString(), anyString(), eq("CLIENT"))).thenReturn("token");

        authService.register(request);

        ArgumentCaptor<Client> clientCaptor = ArgumentCaptor.forClass(Client.class);
        verify(clientRepository).save(clientCaptor.capture());
        assertEquals("EMAIL", clientCaptor.getValue().getNotificationPreference());
    }
}
