package com.buyapp.user.service;

import com.buyapp.user.dto.UserDto;
import com.buyapp.user.dto.UserUpdateRequest;
import com.buyapp.user.entity.Role;
import com.buyapp.user.entity.User;
import com.buyapp.user.exception.AppException;
import com.buyapp.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;
    @InjectMocks private UserService userService;

    private static final String USER_ID = "user-001";

    private User buildUser() {
        User u = new User();
        u.setId(USER_ID);
        u.setUsername("johndoe");
        u.setEmail("john@test.com");
        u.setRole(Role.CLIENT);
        return u;
    }

    @Test
    void getMe_success() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(buildUser()));

        UserDto result = userService.getMe(USER_ID);

        assertThat(result.getId()).isEqualTo(USER_ID);
        assertThat(result.getUsername()).isEqualTo("johndoe");
        assertThat(result.getEmail()).isEqualTo("john@test.com");
    }

    @Test
    void getMe_notFound_throws404() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getMe(USER_ID))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void updateMe_noUsernameChange_success() {
        User user = buildUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UserUpdateRequest req = new UserUpdateRequest();
        req.setAvatarUrl("http://example.com/avatar.png");

        UserDto result = userService.updateMe(USER_ID, req);

        assertThat(result.getAvatarUrl()).isEqualTo("http://example.com/avatar.png");
        verify(kafkaTemplate, never()).send(anyString(), any());
    }

    @Test
    void updateMe_usernameChanged_publishesEvent() {
        User user = buildUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("newname")).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UserUpdateRequest req = new UserUpdateRequest();
        req.setUsername("newname");

        userService.updateMe(USER_ID, req);

        verify(kafkaTemplate, times(1)).send(eq("user-events"), any());
    }

    @Test
    void updateMe_usernameTaken_throws409() {
        User user = buildUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("taken")).thenReturn(true);

        UserUpdateRequest req = new UserUpdateRequest();
        req.setUsername("taken");

        assertThatThrownBy(() -> userService.updateMe(USER_ID, req))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Username already taken");
    }

    @Test
    void deleteMe_success_publishesDeleteEvent() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(buildUser()));

        userService.deleteMe(USER_ID);

        verify(userRepository).delete(any());
        verify(kafkaTemplate).send(eq("user-events"), any());
    }
}
