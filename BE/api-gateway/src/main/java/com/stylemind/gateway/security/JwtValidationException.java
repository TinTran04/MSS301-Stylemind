package com.stylemind.gateway.security;

public class JwtValidationException extends RuntimeException {

    private final JwtValidationCode code;

    public JwtValidationException(JwtValidationCode code, String message) {
        super(message);
        this.code = code;
    }

    public JwtValidationCode getCode() {
        return code;
    }
}
