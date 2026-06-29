package com.stylemind.gateway.security;

import java.time.Instant;

public record GatewayTokenClaims(String userId, String role, String type, String jwtId, Instant issuedAt, Instant expiresAt) {
}
