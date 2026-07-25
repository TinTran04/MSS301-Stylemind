package com.stylemind.gateway.config;

import com.stylemind.gateway.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

    @Bean
    public JwtUtil jwtUtil(@Value("${jwt.public-key-path}") String publicKeyPath) {
        return new JwtUtil(publicKeyPath);
    }
}
