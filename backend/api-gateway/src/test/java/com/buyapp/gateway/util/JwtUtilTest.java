package com.buyapp.gateway.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    // 32-byte key base64-encoded — matches what production uses
    private static final String TEST_SECRET = "dGVzdFNlY3JldEZvclRlc3RpbmdQdXJwb3NlczEyMzQ1Njc4";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", TEST_SECRET);
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(Base64.getDecoder().decode(TEST_SECRET));
    }

    private String buildToken(String userId, String role, String email) {
        return Jwts.builder()
                .subject(userId)
                .claim("role", role)
                .claim("email", email)
                .expiration(new Date(System.currentTimeMillis() + 86400000L))
                .signWith(signingKey())
                .compact();
    }

    private String buildExpiredToken(String userId) {
        return Jwts.builder()
                .subject(userId)
                .claim("role", "CLIENT")
                .expiration(new Date(System.currentTimeMillis() - 3600000L))
                .signWith(signingKey())
                .compact();
    }

    @Test
    void isTokenValid_validToken_returnsTrue() {
        String token = buildToken("user-1", "CLIENT", "test@test.com");
        assertThat(jwtUtil.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_expiredToken_returnsFalse() {
        String token = buildExpiredToken("user-1");
        assertThat(jwtUtil.isTokenValid(token)).isFalse();
    }

    @Test
    void isTokenValid_randomString_returnsFalse() {
        assertThat(jwtUtil.isTokenValid("not.a.token")).isFalse();
    }

    @Test
    void isTokenValid_wrongSignature_returnsFalse() {
        SecretKey wrongKey = Keys.hmacShaKeyFor(
                Base64.getDecoder().decode("d3JvbmdTZWNyZXRLZXlGb3JUZXN0aW5nUHVycG9zZXMhISE="));
        String token = Jwts.builder()
                .subject("user-1")
                .expiration(new Date(System.currentTimeMillis() + 86400000L))
                .signWith(wrongKey)
                .compact();
        assertThat(jwtUtil.isTokenValid(token)).isFalse();
    }

    @Test
    void extractUserId_returnsSubject() {
        String token = buildToken("user-123", "CLIENT", "test@test.com");
        assertThat(jwtUtil.extractUserId(token)).isEqualTo("user-123");
    }

    @Test
    void extractRole_returnsRole() {
        String token = buildToken("user-1", "SELLER", "seller@test.com");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("SELLER");
    }

    @Test
    void extractEmail_returnsEmail() {
        String token = buildToken("user-1", "CLIENT", "john@test.com");
        assertThat(jwtUtil.extractEmail(token)).isEqualTo("john@test.com");
    }
}
