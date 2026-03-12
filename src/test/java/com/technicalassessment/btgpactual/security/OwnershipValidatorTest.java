package com.technicalassessment.btgpactual.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OwnershipValidatorTest {

    private final OwnershipValidator ownershipValidator = new OwnershipValidator();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAllowClientAccessToOwnResources() {
        // CA-02: cliente solo accede a sus propios recursos
        setAuthContext("user-001", "client-001", "CLIENT");

        assertDoesNotThrow(() -> ownershipValidator.validateOwnership("client-001"));
    }

    @Test
    void shouldDenyClientAccessToOtherResources() {
        // CA-02: cliente NO puede acceder a recursos de otro
        setAuthContext("user-001", "client-001", "CLIENT");

        assertThrows(AccessForbiddenException.class,
                () -> ownershipValidator.validateOwnership("client-999"));
    }

    @Test
    void shouldAllowAdminAccessToAnyResources() {
        // CA-03: ADMIN puede acceder a cualquier recurso
        setAuthContext("admin-001", null, "ADMIN");

        assertDoesNotThrow(() -> ownershipValidator.validateOwnership("client-001"));
        assertDoesNotThrow(() -> ownershipValidator.validateOwnership("client-999"));
    }

    @Test
    void shouldDenyUnauthenticatedAccess() {
        assertThrows(AccessForbiddenException.class,
                () -> ownershipValidator.validateOwnership("client-001"));
    }

    private void setAuthContext(String userId, String clientId, String role) {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
        var auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);
        auth.setDetails(new JwtUserDetails(userId, clientId, role));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
