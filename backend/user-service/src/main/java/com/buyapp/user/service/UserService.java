package com.buyapp.user.service;

import com.buyapp.user.dto.UserDto;
import com.buyapp.user.dto.UserUpdateRequest;
import com.buyapp.user.entity.User;
import com.buyapp.user.event.UserEvent;
import com.buyapp.user.exception.AppException;
import com.buyapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public UserDto getMe(String userId) {
        User user = findById(userId);
        return UserDto.from(user);
    }

    public UserDto updateMe(String userId, UserUpdateRequest request) {
        User user = findById(userId);
        boolean usernameChanged = false;

        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            if (!request.getUsername().equals(user.getUsername())) {
                if (userRepository.existsByUsername(request.getUsername())) {
                    throw new AppException("Username already taken", HttpStatus.CONFLICT);
                }
                user.setUsername(request.getUsername());
                usernameChanged = true;
            }
        }

        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        UserDto saved = UserDto.from(userRepository.save(user));

        if (usernameChanged) {
            try {
                kafkaTemplate.send("user-events", UserEvent.builder()
                        .eventType("USER_UPDATED")
                        .userId(userId)
                        .username(saved.getUsername())
                        .timestamp(LocalDateTime.now())
                        .build());
            } catch (Exception e) {
                log.warn("Failed to publish user update event: {}", e.getMessage());
            }
        }

        return saved;
    }

    public void deleteMe(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
        userRepository.delete(user);
        log.info("User {} deleted their account", userId);

        try {
            kafkaTemplate.send("user-events", UserEvent.builder()
                    .eventType("USER_DELETED")
                    .userId(userId)
                    .role(user.getRole().name())
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.warn("Failed to publish user deleted event: {}", e.getMessage());
        }
    }

    private User findById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
    }
}
