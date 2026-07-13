package com.stylemind.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Slf4j
public class JwtUtil {

    // Immutable key fields (set once during construction)
    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    
    // Pre-compiled parser/builder for zero-I/O runtime performance
    private final JwtParser jwtParser;
    private final JwtBuilder jwtBuilder;
    
    // Token expiration settings
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;
    
    // Constructor for Issuer mode (RSA-2048 signing)
    public JwtUtil(PrivateKey privateKey, PublicKey publicKey, 
                   long accessTokenExpiration, long refreshTokenExpiration) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
        
        // Pre-compile parser with public key for verification
        // Issuer must have public key for verification (PrivateKey cannot verify signatures)
        if (publicKey == null) {
            throw new IllegalArgumentException("PublicKey is required for token verification in issuer mode");
        }
        this.jwtParser = Jwts.parser()
            .verifyWith(publicKey)
            .build();
        
        // Pre-compile builder with private key for signing
        this.jwtBuilder = Jwts.builder()
            .signWith(privateKey, SignatureAlgorithm.RS256);
    }
    
    // Constructor for Consumer mode (RSA-2048 verification only)
    public JwtUtil(PublicKey publicKey, long accessTokenExpiration, long refreshTokenExpiration) {
        this.privateKey = null;
        this.publicKey = publicKey;
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
        
        // Pre-compile parser with public key for verification
        this.jwtParser = Jwts.parser()
            .verifyWith(publicKey)
            .build();
        
        // Builder not needed in consumer mode (no signing capability)
        this.jwtBuilder = null;
    }
    
    // Getters for expiration settings
    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }
    
    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    public String generateAccessToken(UserDetails userDetails, String userId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);
        return createToken(claims, userDetails.getUsername(), accessTokenExpiration);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return createToken(new HashMap<>(), userDetails.getUsername(), refreshTokenExpiration);
    }

    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        if (jwtBuilder == null) {
            throw new IllegalStateException("JwtUtil not configured for signing (consumer mode)");
        }
        
        return jwtBuilder
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .compact();
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", String.class));
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        // Use pre-compiled JwtParser - zero I/O during runtime
        return jwtParser.parseSignedClaims(token).getPayload();
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }
}
