package org.example.eventplatform.event.config;

import lombok.RequiredArgsConstructor;
import org.example.eventplatform.shared.security.JwtAuthenticationFilter;
import org.example.eventplatform.shared.security.JwtTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    @org.springframework.beans.factory.annotation.Value("${internal.service-token:}")
    private String internalServiceToken;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // Sàn/khách tạo show request công khai; xem plan "sàn đẩy show"
                        .requestMatchers(HttpMethod.POST, "/api/events").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        // Chỉ service khác gọi (không đi qua gateway); InternalServiceAuthFilter kiểm tra X-Internal-Token.
                        .requestMatchers("/api/internal/**").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        http.addFilterBefore(
                new org.example.eventplatform.shared.security.InternalServiceAuthFilter(internalServiceToken),
                UsernamePasswordAuthenticationFilter.class
        );
        http.addFilterBefore(
                new JwtAuthenticationFilter(jwtTokenProvider),
                UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }
}
