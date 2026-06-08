package com.buyapp.user.security;

import com.buyapp.user.entity.Role;
import com.buyapp.user.entity.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        String secret = Base64.getEncoder().encodeToString(
                "test-secret-key-that-is-long-enough-for-hmac-sha256!!".getBytes(StandardCharsets.UTF_8)
        );
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", secret);
        ReflectionTestUtils.setField(jwtUtil, "jwtExpiration", 86400000L);

        testUser = new User();
        testUser.setId("user-123");
        testUser.setUsername("johndoe");
        testUser.setEmail("john@test.com");
        testUser.setRole(Role.CLIENT);
    }

    @Test
    void generateToken_returnsNonNullToken() {
        String token = jwtUtil.generateToken(testUser);
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void extractUserId_returnsCorrectSubject() {
        String token = jwtUtil.generateToken(testUser);
        assertThat(jwtUtil.extractUserId(token)).isEqualTo("user-123");
    }

    @Test
    void extractAllClaims_containsEmailAndRole() {
        String token = jwtUtil.generateToken(testUser);
        Claims claims = jwtUtil.extractAllClaims(token);

        assertThat(claims.get("email", String.class)).isEqualTo("john@test.com");
        assertThat(claims.get("username", String.class)).isEqualTo("johndoe");
        assertThat(claims.get("role", String.class)).isEqualTo("CLIENT");
    }

    @Test
    void isTokenValid_validToken_returnsTrue() {
        String token = jwtUtil.generateToken(testUser);
        assertThat(jwtUtil.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_tamperedToken_returnsFalse() {
        String token = jwtUtil.generateToken(testUser) + "tampered";
        assertThat(jwtUtil.isTokenValid(token)).isFalse();
    }

    @Test
    void isTokenValid_randomString_returnsFalse() {
        assertThat(jwtUtil.isTokenValid("not.a.token")).isFalse();
    }

    @Test
    void generateToken_differentUsers_differentTokens() {
        User other = new User();
        other.setId("user-456");
        other.setUsername("jane");
        other.setEmail("jane@test.com");
        other.setRole(Role.SELLER);

        String token1 = jwtUtil.generateToken(testUser);
        String token2 = jwtUtil.generateToken(other);

        assertThat(token1).isNotEqualTo(token2);
        assertThat(jwtUtil.extractUserId(token2)).isEqualTo("user-456");
    }
}
