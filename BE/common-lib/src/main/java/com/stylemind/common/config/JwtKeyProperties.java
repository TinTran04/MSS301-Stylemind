package com.stylemind.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtKeyProperties {

    // Asymmetric key paths (RSA-2048)
    private String privateKeyPath;
    private String publicKeyPath;
    
    // Symmetric key (HMAC-SHA256) - for backward compatibility during transition
    private String secret;
    
    // Algorithm configuration
    private String algorithm = "RSA";
    private Integer keySize = 2048;
    
    // Token expiration settings
    private Long accessTokenExpiration = 3600000L;
    private Long refreshTokenExpiration = 604800000L;
}
