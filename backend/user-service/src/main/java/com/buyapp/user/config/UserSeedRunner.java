package com.buyapp.user.config;

import com.buyapp.user.entity.Role;
import com.buyapp.user.entity.User;
import com.buyapp.user.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
@Slf4j
public class UserSeedRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @Value("${app.seed.force:false}")
    private boolean force;

    @Value("${app.seed.users-resource:seed/users.json}")
    private String usersResource;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!force && userRepository.count() > 0) {
            log.info("Skipping user seed because the users collection is not empty.");
            return;
        }

        List<SeedUser> seedUsers = readUsers();
        if (seedUsers.isEmpty()) {
            log.warn("No user seed data was loaded from {}.", usersResource);
            return;
        }

        if (force) {
            userRepository.deleteAll();
        }

        List<User> users = seedUsers.stream()
                .map(this::toUser)
                .toList();

        userRepository.saveAll(users);
        log.info("Seeded {} users from {}.", users.size(), usersResource);
    }

    private List<SeedUser> readUsers() throws IOException {
        ClassPathResource resource = new ClassPathResource(usersResource);
        if (!resource.exists()) {
            return List.of();
        }

        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, new TypeReference<List<SeedUser>>() {
            });
        }
    }

    private User toUser(SeedUser seedUser) {
        return User.builder()
                .id(seedUser.id())
                .username(seedUser.username())
                .email(seedUser.email())
                .password(passwordEncoder.encode(seedUser.password()))
                .role(seedUser.role())
                .avatarUrl(seedUser.avatarUrl())
                .build();
    }

    private record SeedUser(
            String id,
            String username,
            String email,
            String password,
            Role role,
            String avatarUrl
    ) {
    }
}
