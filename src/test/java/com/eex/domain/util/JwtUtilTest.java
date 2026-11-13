package com.eex.domain.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef0123456789";
    private static final String OTHER_SECRET = "ZYXWVUTSRQPONMLKJIHGFEDCBA9876543210zyxwvu";

    private final JwtUtil jwtUtil = new JwtUtil(SECRET, 60_000);

    @Test
    void generatedTokenIsValidAndCarriesUserId() {
        String token = jwtUtil.generateToken(42);

        assertThat(jwtUtil.validateAndGetUserId(token)).isEqualTo(42);
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        String foreignToken = new JwtUtil(OTHER_SECRET, 60_000).generateToken(1);

        assertThat(jwtUtil.validateAndGetUserId(foreignToken)).isNull();
    }

    @Test
    void expiredTokenIsRejected() {
        String expired = Jwts.builder()
                .subject("1")
                .issuedAt(new Date(System.currentTimeMillis() - 120_000))
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThat(jwtUtil.validateAndGetUserId(expired)).isNull();
    }

    @Test
    void tamperedTokenIsRejected() {
        // On garde la signature d'origine mais on remplace le payload pour usurper l'utilisateur 2.
        String[] parts = jwtUtil.generateToken(1).split("\\.");
        String forgedPayload = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"2\"}".getBytes(StandardCharsets.UTF_8));
        String tampered = parts[0] + "." + forgedPayload + "." + parts[2];

        assertThat(jwtUtil.validateAndGetUserId(tampered)).isNull();
    }

    @Test
    void unsignedTokenIsRejected() {
        String unsigned = Jwts.builder().subject("1").compact();

        assertThat(jwtUtil.validateAndGetUserId(unsigned)).isNull();
    }

    @Test
    void nonNumericSubjectIsRejected() {
        String token = Jwts.builder()
                .subject("admin")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThat(jwtUtil.validateAndGetUserId(token)).isNull();
    }

    @Test
    void nullBlankOrGarbageTokenIsRejected() {
        assertThat(jwtUtil.validateAndGetUserId(null)).isNull();
        assertThat(jwtUtil.validateAndGetUserId("  ")).isNull();
        assertThat(jwtUtil.validateAndGetUserId("not.a.jwt")).isNull();
    }

    @Test
    void tooShortSecretIsRefusedAtStartup() {
        assertThatThrownBy(() -> new JwtUtil("short", 60_000))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void nonPositiveExpirationIsRefusedAtStartup() {
        assertThatThrownBy(() -> new JwtUtil(SECRET, 0))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void generatingTokenWithoutUserIdFails() {
        assertThatThrownBy(() -> jwtUtil.generateToken(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
