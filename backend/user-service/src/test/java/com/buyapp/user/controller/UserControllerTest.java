package com.buyapp.user.controller;

import com.buyapp.user.dto.UserDto;
import com.buyapp.user.entity.Role;
import com.buyapp.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(TestSecurityConfig.class)
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private UserService userService;

    private static final String USER_ID = "user-001";

    private UsernamePasswordAuthenticationToken clientAuth() {
        return new UsernamePasswordAuthenticationToken(
                USER_ID, null, List.of(new SimpleGrantedAuthority("ROLE_CLIENT")));
    }

    private UserDto buildUserDto() {
        UserDto dto = new UserDto();
        dto.setId(USER_ID);
        dto.setUsername("johndoe");
        dto.setEmail("john@test.com");
        dto.setRole(Role.CLIENT);
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    @Test
    void getMe_authenticated_returns200() throws Exception {
        when(userService.getMe(USER_ID)).thenReturn(buildUserDto());

        mockMvc.perform(get("/users/me").with(authentication(clientAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID))
                .andExpect(jsonPath("$.username").value("johndoe"));
    }

    @Test
    void getMe_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateMe_validRequest_returns200() throws Exception {
        UserDto updated = buildUserDto();
        updated.setAvatarUrl("http://example.com/avatar.png");
        when(userService.updateMe(eq(USER_ID), any())).thenReturn(updated);

        Map<String, Object> body = Map.of("avatarUrl", "http://example.com/avatar.png");

        mockMvc.perform(put("/users/me")
                        .with(authentication(clientAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value("http://example.com/avatar.png"));
    }

    @Test
    void updateMe_usernameTooShort_returns400() throws Exception {
        Map<String, Object> body = Map.of("username", "ab");

        mockMvc.perform(put("/users/me")
                        .with(authentication(clientAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateMe_unauthenticated_returns401() throws Exception {
        mockMvc.perform(put("/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteMe_authenticated_returns204() throws Exception {
        mockMvc.perform(delete("/users/me").with(authentication(clientAuth())))
                .andExpect(status().isNoContent());

        verify(userService).deleteMe(USER_ID);
    }

    @Test
    void deleteMe_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/users/me"))
                .andExpect(status().isUnauthorized());
    }
}
