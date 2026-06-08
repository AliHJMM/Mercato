package com.buyapp.user.controller;

import com.buyapp.user.dto.AuthResponse;
import com.buyapp.user.dto.LoginRequest;
import com.buyapp.user.dto.RegisterRequest;
import com.buyapp.user.entity.Role;
import com.buyapp.user.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class)
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private AuthService authService;

    private AuthResponse buildResponse() {
        AuthResponse r = new AuthResponse();
        r.setToken("jwt-token");
        r.setId("user-1");
        r.setUsername("johndoe");
        r.setEmail("john@test.com");
        r.setRole(Role.CLIENT);
        return r;
    }

    @Test
    void register_validRequest_returns201() throws Exception {
        when(authService.register(any())).thenReturn(buildResponse());

        Map<String, Object> body = Map.of(
                "username", "johndoe",
                "email", "john@test.com",
                "password", "Password1!",
                "role", "CLIENT"
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.email").value("john@test.com"));
    }

    @Test
    void register_invalidEmail_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "username", "johndoe",
                "email", "not-an-email",
                "password", "Password1!",
                "role", "CLIENT"
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_weakPassword_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "username", "johndoe",
                "email", "john@test.com",
                "password", "weakpass",
                "role", "CLIENT"
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_missingRole_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "username", "johndoe",
                "email", "john@test.com",
                "password", "Password1!"
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_usernameTooShort_returns400() throws Exception {
        Map<String, Object> body = Map.of(
                "username", "ab",
                "email", "john@test.com",
                "password", "Password1!",
                "role", "CLIENT"
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_validRequest_returns200() throws Exception {
        when(authService.login(any())).thenReturn(buildResponse());

        Map<String, Object> body = Map.of(
                "email", "john@test.com",
                "password", "Password1!"
        );

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void login_missingEmail_returns400() throws Exception {
        Map<String, Object> body = Map.of("password", "Password1!");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_missingPassword_returns400() throws Exception {
        Map<String, Object> body = Map.of("email", "john@test.com");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }
}
