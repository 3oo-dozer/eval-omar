package com.eex.application.controller;

import com.eex.application.dto.AuthResponse;
import com.eex.application.dto.LoginRequest;
import com.eex.domain.entity.User;
import com.eex.domain.service.UserService;
import com.eex.domain.util.JwtUtil;
import com.eex.domain.util.PasswordUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private static final String INVALID_CREDENTIALS = "Identifiants invalides";

    /** Hash factice : on exécute BCrypt même si l'utilisateur n'existe pas (temps de réponse constant). */
    private static final String DUMMY_HASH = PasswordUtil.hashPassword("dummy-password-for-timing");

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        List<User> users = userService.getUsersByUsername(loginRequest.getUsername());
        User user = users.size() == 1 ? users.get(0) : null;

        String hash = user != null ? user.getPassword() : DUMMY_HASH;
        boolean passwordOk = PasswordUtil.matches(loginRequest.getPassword(), hash);

        if (user == null || !passwordOk) {
            logger.warn("Échec de connexion");
            return new ResponseEntity<>(new AuthResponse(null, INVALID_CREDENTIALS), HttpStatus.UNAUTHORIZED);
        }

        String token = jwtUtil.generateToken(user.getId());
        logger.info("Connexion réussie pour l'utilisateur id={}", user.getId());
        return new ResponseEntity<>(new AuthResponse(token, "Authentification réussie"), HttpStatus.OK);
    }

    @PostMapping("/validate")
    public ResponseEntity<AuthResponse> validateToken(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return new ResponseEntity<>(new AuthResponse(null, "Jeton manquant ou invalide"), HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7);
        Integer userId = jwtUtil.validateAndGetUserId(token);

        if (userId == null || userService.getUserById(userId).isEmpty()) {
            return new ResponseEntity<>(new AuthResponse(null, "Jeton invalide ou expiré"), HttpStatus.UNAUTHORIZED);
        }

        return new ResponseEntity<>(new AuthResponse(token, "Jeton valide"), HttpStatus.OK);
    }
}
