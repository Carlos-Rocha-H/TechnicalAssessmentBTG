package com.technicalassessment.btgpactual.service;

import com.technicalassessment.btgpactual.dto.LoginRequest;
import com.technicalassessment.btgpactual.dto.LoginResponse;
import com.technicalassessment.btgpactual.dto.RegisterRequest;
import com.technicalassessment.btgpactual.model.Client;
import com.technicalassessment.btgpactual.model.User;
import com.technicalassessment.btgpactual.repository.ClientRepository;
import com.technicalassessment.btgpactual.repository.UserRepository;
import com.technicalassessment.btgpactual.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository,
                       ClientRepository clientRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.clientRepository = clientRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Credenciales inválidas");
        }

        String token = jwtUtil.generateToken(user.getUserId(), user.getClientId(), user.getRole());

        return LoginResponse.builder()
                .token(token)
                .userId(user.getUserId())
                .clientId(user.getClientId())
                .role(user.getRole())
                .build();
    }

    public LoginResponse register(RegisterRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Username y password son obligatorios");
        }

        userRepository.findByUsername(request.getUsername())
                .ifPresent(u -> {
                    throw new IllegalArgumentException("El username '" + request.getUsername() + "' ya está registrado");
                });

        String clientId = UUID.randomUUID().toString();
        String notificationPref = (request.getNotificationPreference() != null)
                ? request.getNotificationPreference() : "EMAIL";

        Client client = Client.builder()
                .clientId(clientId)
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .notificationPreference(notificationPref)
                .balance(500000.0)
                .build();
        clientRepository.save(client);

        String userId = UUID.randomUUID().toString();
        User user = User.builder()
                .userId(userId)
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role("CLIENT")
                .clientId(clientId)
                .build();
        userRepository.save(user);

        String token = jwtUtil.generateToken(userId, clientId, "CLIENT");

        return LoginResponse.builder()
                .token(token)
                .userId(userId)
                .clientId(clientId)
                .role("CLIENT")
                .build();
    }
}
