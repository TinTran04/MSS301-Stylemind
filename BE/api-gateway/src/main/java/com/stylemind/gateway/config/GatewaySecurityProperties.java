package com.stylemind.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "gateway.security")
public class GatewaySecurityProperties {

    private String internalServiceSecret;
    private Jwt jwt = new Jwt();

    public String getInternalServiceSecret() {
        return internalServiceSecret;
    }

    public void setInternalServiceSecret(String internalServiceSecret) {
        this.internalServiceSecret = internalServiceSecret;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public static class Jwt {
        private String publicKey;
        private String publicKeyPath;
        private String secret;

        public String getPublicKey() {
            return publicKey;
        }

        public void setPublicKey(String publicKey) {
            this.publicKey = publicKey;
        }

        public String getPublicKeyPath() {
            return publicKeyPath;
        }

        public void setPublicKeyPath(String publicKeyPath) {
            this.publicKeyPath = publicKeyPath;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }
    }
}
