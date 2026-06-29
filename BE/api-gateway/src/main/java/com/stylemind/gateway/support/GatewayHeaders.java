package com.stylemind.gateway.support;

public final class GatewayHeaders {

    public static final String REQUEST_ID = "X-Request-Id";
    public static final String USER_ID = "X-User-Id";
    public static final String USER_ROLE = "X-User-Role";
    public static final String TOKEN_ID = "X-Token-Id";
    public static final String TOKEN_TYPE = "X-Token-Type";
    public static final String LEGACY_USER_ROLES = "X-User-Roles";
    public static final String INTERNAL_REQUEST = "X-Internal-Request";
    public static final String INTERNAL_TOKEN = "X-Internal-Token";
    public static final String INTERNAL_SERVICE_SECRET = "X-Internal-Service-Secret";

    private GatewayHeaders() {
    }
}
