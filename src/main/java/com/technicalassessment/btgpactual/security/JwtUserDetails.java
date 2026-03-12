package com.technicalassessment.btgpactual.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class JwtUserDetails {
    private final String userId;
    private final String clientId;
    private final String role;
}
