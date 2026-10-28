package com.deroyque.activation.infrastructure.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import java.time.Instant;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/requests").hasAuthority("SCOPE_activations:write")
                        .requestMatchers(HttpMethod.GET, "/api/v1/requests/**").hasAuthority("SCOPE_activations:read")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))
                .build();
    }

    /**
     * Local/Development profile JwtDecoder allowing developer testing with mock tokens
     * without requiring an external OAuth 2.0 authorization server.
     */
    @Bean
    @Profile({"dev", "local"})
    public JwtDecoder devJwtDecoder() {
        return token -> Jwt.withTokenValue(token)
                .header("alg", "none")
                .claim("sub", "dev-client")
                .claim("scope", "activations:read activations:write")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(86400))
                .build();
    }
}
