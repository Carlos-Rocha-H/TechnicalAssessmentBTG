package com.technicalassessment.btgpactual.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private static final String SECRET = "btgpactual-super-secret-key-for-jwt-token-signing-256bits!!";
    private final JwtUtil jwtUtil = new JwtUtil(SECRET, 3600000);

    @Test
    void shouldGenerateValidToken() {
        String token = jwtUtil.generateToken("user-001", "client-001", "CLIENT");

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(jwtUtil.isTokenValid(token));
    }

    @Test
    void shouldExtractCorrectClaims() {
        String token = jwtUtil.generateToken("user-001", "client-001", "CLIENT");

        assertEquals("user-001", jwtUtil.getUserId(token));
        assertEquals("client-001", jwtUtil.getClientId(token));
        assertEquals("CLIENT", jwtUtil.getRole(token));
    }

    @Test
    void shouldExtractAdminClaims() {
        String token = jwtUtil.generateToken("admin-001", null, "ADMIN");

        assertEquals("admin-001", jwtUtil.getUserId(token));
        assertNull(jwtUtil.getClientId(token));
        assertEquals("ADMIN", jwtUtil.getRole(token));
    }

    @Test
    void shouldReturnFalseForInvalidToken() {
        assertFalse(jwtUtil.isTokenValid("invalid-token"));
    }

    @Test
    void shouldReturnFalseForExpiredToken() {
        JwtUtil expiredJwtUtil = new JwtUtil(SECRET, 0);
        String token = expiredJwtUtil.generateToken("user-001", "client-001", "CLIENT");

        // Token con expiración 0ms ya está expirado
        assertFalse(jwtUtil.isTokenValid(token));
    }

    @Test
    void shouldParseTokenClaims() {
        String token = jwtUtil.generateToken("user-001", "client-001", "CLIENT");

        Claims claims = jwtUtil.parseToken(token);

        assertEquals("user-001", claims.getSubject());
        assertEquals("client-001", claims.get("clientId", String.class));
        assertEquals("CLIENT", claims.get("role", String.class));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
    }
}
