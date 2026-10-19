package com.example.taskmanager.security;

import com.example.taskmanager.entity.User;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "a-test-secret-that-is-longer-than-32-characters!!";
    private static final String OTHER_SECRET = "a-completely-different-secret-longer-than-32-chars!!";

    private static User user(Long id) {
        User user = new User("Alice", "alice@example.com", "$2a$hash");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    @DisplayName("a generated token validates and carries the user id")
    void generateAndParse() {
        JwtService jwtService = new JwtService(SECRET, 3_600_000L);

        String token = jwtService.generateToken(user(42L));

        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractUserId(token)).isEqualTo(42L);
    }

    @Test
    @DisplayName("a tampered token is rejected")
    void tamperedTokenIsInvalid() {
        JwtService jwtService = new JwtService(SECRET, 3_600_000L);
        String token = jwtService.generateToken(user(1L));

        String tampered = token.substring(0, token.length() - 2) + "xy";

        assertThat(jwtService.isTokenValid(tampered)).isFalse();
        assertThatThrownBy(() -> jwtService.extractUserId(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("a token signed with a different secret is rejected")
    void wrongSignatureIsInvalid() {
        JwtService issuer = new JwtService(SECRET, 3_600_000L);
        JwtService verifier = new JwtService(OTHER_SECRET, 3_600_000L);

        String token = issuer.generateToken(user(1L));

        assertThat(verifier.isTokenValid(token)).isFalse();
        assertThatThrownBy(() -> verifier.extractUserId(token)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("an expired token is rejected")
    void expiredTokenIsInvalid() {
        // Negative lifetime means the token is already expired when created.
        JwtService jwtService = new JwtService(SECRET, -1_000L);

        String token = jwtService.generateToken(user(7L));

        assertThat(jwtService.isTokenValid(token)).isFalse();
        assertThatThrownBy(() -> jwtService.extractUserId(token)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("a too-short secret makes startup fail with a helpful message")
    void shortSecretFails() {
        assertThatThrownBy(() -> new JwtService("too-short", 3_600_000L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }
}
