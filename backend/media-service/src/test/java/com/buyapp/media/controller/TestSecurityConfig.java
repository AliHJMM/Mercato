package com.buyapp.media.controller;

import com.buyapp.media.security.JwtUtil;
import com.mongodb.client.MongoClient;
import jakarta.servlet.http.HttpServletResponse;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@TestConfiguration
@EnableMethodSecurity
public class TestSecurityConfig {

    @Bean
    public JwtUtil jwtUtil() {
        return Mockito.mock(JwtUtil.class);
    }

    @Bean
    public MongoClient mongoClient() {
        return Mockito.mock(MongoClient.class);
    }

    @Bean
    @Primary
    public MongoMappingContext mongoMappingContext() {
        MongoMappingContext context = new MongoMappingContext();
        context.setAutoIndexCreation(false);
        return context;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) ->
                                res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("GET", "/media/images/**").permitAll()
                        .anyRequest().authenticated()
                )
                .build();
    }
}
