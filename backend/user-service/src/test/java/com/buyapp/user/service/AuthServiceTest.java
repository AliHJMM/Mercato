package com.buyapp.user.service;

import com.buyapp.user.dto.AuthResponse;
import com.buyapp.user.dto.LoginRequest;
import com.buyapp.user.dto.RegisterRequest;
import com.buyapp.user.entity.Role;
import com.buyapp.user.entity.User;
import com.buyapp.user.exception.AppException;
import com.buyapp.user.repository.UserRepository;
import com.buyapp.user.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @InjectMocks private AuthService authService;

    private RegisterRequest buildRegisterRequest() {
        RegisterRequest r = new RegisterRequest();
        r.setUsername("johndoe");
        r.setEmail("john@test.com");
        r.setPassword("Password1!");
        r.setRole(Role.CLIENT);
        return r;
    }

    private User buildUser() {
        User u = new User();
        u.setId("user-1");
        u.setUsername("johndoe");
        u.setEmail("john@test.com");
        u.setPassword("encoded");
        u.setRole(Role.CLIENT);
        return u;
    }

    @Test
    void register_success() {
        when(userRepository.existsByEmail("john@test.com")).thenReturn(false);
        when(userRepository.existsByUsername("johndoe")).thenReturn(false);
        when(passwordEncoder.encode("Password1!")).thenReturn("encoded");
        when(userRepository.save(any())).thenReturn(buildUser());
        when(jwtUtil.generateToken(any())).thenReturn("jwt-token");

        AuthResponse result = authService.register(buildRegisterRequest());

        assertThat(result.getToken()).isEqualTo("jwt-token");
        assertThat(result.getEmail()).isEqualTo("john@test.com");
        assertThat(result.getRole()).isEqualTo(Role.CLIENT);
        verify(kafkaTemplate).send(eq("user-events"), any());
    }

    @Test
    void register_emailAlreadyExists_throws409() {
        when(userRepository.existsByEmail("john@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(buildRegisterRequest()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Email already in use");
    }

    @Test
    void register_usernameAlreadyExists_throws409() {
        when(userRepository.existsByEmail("john@test.com")).thenReturn(false);
        when(userRepository.existsByUsername("johndoe")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(buildRegisterRequest()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Username already taken");
    }

    @Test
    void login_success() {
        User user = buildUser();
        LoginRequest request = new LoginRequest();
        request.setEmail("john@test.com");
        request.setPassword("Password1!");

        when(userRepository.findByEmail("john@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password1!", "encoded")).thenReturn(true);
        when(jwtUtil.generateToken(user)).thenReturn("jwt-token");

        AuthResponse result = authService.login(request);

        assertThat(result.getToken()).isEqualTo("jwt-token");
        assertThat(result.getUsername()).isEqualTo("johndoe");
    }

    @Test
    void login_emailNotFound_throws401() {
        LoginRequest request = new LoginRequest();
        request.setEmail("nobody@test.com");
        request.setPassword("Password1!");

        when(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Invalid");
    }

    @Test
    void login_wrongPassword_throws401() {
        User user = buildUser();
        LoginRequest request = new LoginRequest();
        request.setEmail("john@test.com");
        request.setPassword("Wrong1!");

        when(userRepository.findByEmail("john@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Wrong1!", "encoded")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Invalid");
    }

    @Test
    void register_publishesUserRegisteredEvent() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(any())).thenReturn(buildUser());
        when(jwtUtil.generateToken(any())).thenReturn("token");

        authService.register(buildRegisterRequest());

        verify(kafkaTemplate, times(1)).send(eq("user-events"), any());
    }
}
