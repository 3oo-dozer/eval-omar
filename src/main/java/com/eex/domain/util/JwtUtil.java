package com.eex.domain.util;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    /** HS256 exige une clé d'au moins 256 bits. */
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey signingKey;
    private final long jwtExpiration;

    public JwtUtil(@Value("${jwt.secret}") String jwtSecret,
                   @Value("${jwt.expiration}") long jwtExpiration) {
        if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("jwt.secret doit faire au moins " + MIN_SECRET_BYTES + " octets");
        }
        if (jwtExpiration <= 0) {
            throw new IllegalStateException("jwt.expiration doit être strictement positif");
        }
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.jwtExpiration = jwtExpiration;
    }

    public String generateToken(Integer userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId obligatoire");
        }
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + jwtExpiration))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * @return l'id utilisateur porté par le jeton, ou {@code null} si le jeton est absent,
     * mal formé, mal signé ou expiré.
     */
    public Integer validateAndGetUserId(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            String subject = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
            return Integer.valueOf(subject);
        } catch (JwtException | IllegalArgumentException e) {
            logger.debug("Jeton rejeté : {}", e.getMessage());
            return null;
        }
    }
}
