package com.technicalassessment.btgpactual.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class OwnershipValidator {

    public void validateOwnership(String pathClientId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getDetails() == null) {
            throw new AccessForbiddenException("No autenticado");
        }

        JwtUserDetails details = (JwtUserDetails) authentication.getDetails();

        // ADMIN puede acceder a cualquier recurso
        if ("ADMIN".equals(details.getRole())) {
            return;
        }

        // CLIENT solo puede acceder a sus propios recursos
        if (!pathClientId.equals(details.getClientId())) {
            throw new AccessForbiddenException(
                    "No tiene permisos para acceder a los recursos del cliente " + pathClientId);
        }
    }
}
