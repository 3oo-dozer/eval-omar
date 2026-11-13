package com.eex.application.controller;

import com.eex.application.dto.AuthResponse;
import com.eex.application.dto.LoginRequest;
import com.eex.domain.entity.User;
import com.eex.domain.service.UserService;
import com.eex.domain.util.JwtUtil;
import com.eex.domain.util.PasswordUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private static final String ALICE_HASH = PasswordUtil.hashPassword("aliceSecret");

    @Mock
    private UserService userService;
    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthController authController;

    @Test
    void loginWithValidCredentialsReturnsToken() {
        when(userService.getUsersByUsername("Alice")).thenReturn(List.of(new User(1, "Alice", ALICE_HASH, 1)));
        when(jwtUtil.generateToken(1)).thenReturn("jwt");

        ResponseEntity<AuthResponse> response = authController.login(new LoginRequest("Alice", "aliceSecret"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getToken()).isEqualTo("jwt");
    }

    @Test
    void loginWithWrongPasswordIsUnauthorized() {
        when(userService.getUsersByUsername("Alice")).thenReturn(List.of(new User(1, "Alice", ALICE_HASH, 1)));

        ResponseEntity<AuthResponse> response = authController.login(new LoginRequest("Alice", "wrong"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getToken()).isNull();
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    void loginWithUnknownUserIsUnauthorizedWithSameMessage() {
        when(userService.getUsersByUsername("Nobody")).thenReturn(List.of());
        when(userService.getUsersByUsername("Alice")).thenReturn(List.of(new User(1, "Alice", ALICE_HASH, 1)));

        ResponseEntity<AuthResponse> unknown = authController.login(new LoginRequest("Nobody", "x"));
        ResponseEntity<AuthResponse> wrongPwd = authController.login(new LoginRequest("Alice", "x"));

        assertThat(unknown.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        // Même message : on ne révèle pas si l'identifiant existe.
        assertThat(unknown.getBody().getMessage()).isEqualTo(wrongPwd.getBody().getMessage());
    }

    @Test
    void loginIsRefusedWhenUsernameIsAmbiguous() {
        when(userService.getUsersByUsername("Alice")).thenReturn(List.of(
                new User(1, "Alice", ALICE_HASH, 1),
                new User(6, "Alice", ALICE_HASH, 2)));

        ResponseEntity<AuthResponse> response = authController.login(new LoginRequest("Alice", "aliceSecret"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    void validateWithoutHeaderIsUnauthorized() {
        assertThat(authController.validateToken(null).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(authController.validateToken("Basic abc").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void validateWithInvalidTokenIsUnauthorized() {
        when(jwtUtil.validateAndGetUserId("bad")).thenReturn(null);

        assertThat(authController.validateToken("Bearer bad").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void validateWithTokenOfDeletedUserIsUnauthorized() {
        when(jwtUtil.validateAndGetUserId("t")).thenReturn(99);
        when(userService.getUserById(99)).thenReturn(Optional.empty());

        assertThat(authController.validateToken("Bearer t").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void validateWithValidTokenIsOk() {
        when(jwtUtil.validateAndGetUserId("t")).thenReturn(1);
        when(userService.getUserById(1)).thenReturn(Optional.of(new User(1, "Alice", ALICE_HASH, 1)));

        assertThat(authController.validateToken("Bearer t").getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
