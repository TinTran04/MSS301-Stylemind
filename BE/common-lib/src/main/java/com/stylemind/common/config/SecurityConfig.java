package com.stylemind.common.config;

import com.stylemind.common.security.HeaderAuthenticationFilter;
import com.stylemind.common.security.InternalAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final InternalAuthFilter internalAuthFilter;
    private final HeaderAuthenticationFilter headerAuthenticationFilter;

    public SecurityConfig(InternalAuthFilter internalAuthFilter,
                          HeaderAuthenticationFilter headerAuthenticationFilter) {
        this.internalAuthFilter = internalAuthFilter;
        this.headerAuthenticationFilter = headerAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Browser sends OPTIONS preflight with no auth header — must pass before JWT filter
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/swagger-ui/index.html").permitAll()
                        .requestMatchers("/internal/v1/**").permitAll()
                        .requestMatchers(
                                "/api/v1/auth/login",
                                "/api/v1/auth/register",
                                // Register email-OTP verification/resend: the user has no JWT yet,
                                // so these must be public like the other pre-login auth endpoints.
                                // Exact sub-paths (not /register/**) to keep the whitelist precise.
                                "/api/v1/auth/register/verify-otp",
                                "/api/v1/auth/register/resend-otp",
                                "/api/v1/auth/password/setup",
                                "/api/v1/auth/forgot-password",
                                "/api/v1/auth/verify-reset-otp",
                                "/api/v1/auth/reset-password"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/**", "/api/v1/categories/**").permitAll()
                        .requestMatchers("/api/v1/cart/**").permitAll()
                        // SePay calls this directly (server-to-server, no user JWT) - authenticity
                        // is verified inside PaymentService via the webhook's own API-key header.
                        // Only SePay's public webhook is unauthenticated. Other payment
                        // paths must still require the caller's normal authentication.
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments/webhook/sepay").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(internalAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(headerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // allowedOriginPatterns("*") works with any origin including localhost:5173, swagger-ui, etc.
        // Unlike setAllowedOrigins("*"), patterns also support allowCredentials=true if needed later.
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("X-Request-Id", "X-User-Id", "X-User-Roles"));
        configuration.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
