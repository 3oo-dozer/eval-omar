package com.eex.application.controller;

import com.eex.application.dto.UserResponse;
import com.eex.domain.service.UserService;
import com.eex.infrastructure.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    /** Les utilisateurs du client connecté. */
    @GetMapping
    public List<UserResponse> getMyClientUsers(@AuthenticationPrincipal AuthenticatedUser me) {
        return userService.getUsersByClientId(me.clientId()).stream()
                .map(UserResponse::from)
                .toList();
    }

    /** L'utilisateur connecté. */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal AuthenticatedUser me) {
        return getUserById(me.userId(), me);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Integer id,
            @AuthenticationPrincipal AuthenticatedUser me) {
        return userService.getUserForClient(id, me.clientId())
                .map(UserResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // @GetMapping("/client/{clientId}")
    // public ResponseEntity<List<User>> getUsersByClientId(@PathVariable Integer
    // clientId) {
    // List<User> users = userService.getUsersByClientId(clientId);
    // return new ResponseEntity<>(users, HttpStatus.OK);
    // }

    // @GetMapping("/search")
    // public ResponseEntity<List<User>> getUsersByUsername(@RequestParam String
    // username) {
    // List<User> users = userService.getUsersByUsername(username);
    // return new ResponseEntity<>(users, HttpStatus.OK);
    // }

    // @PostMapping
    // public ResponseEntity<User> createUser(@RequestBody User user) {
    // User createdUser = userService.createUser(user);
    // return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    // }

    // @PutMapping("/{id}")
    // public ResponseEntity<User> updateUser(@PathVariable Integer id, @RequestBody
    // User userDetails) {
    // try {
    // User updatedUser = userService.updateUser(id, userDetails);
    // return new ResponseEntity<>(updatedUser, HttpStatus.OK);
    // } catch (RuntimeException e) {
    // return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    // }
    // }

    // @DeleteMapping("/{id}")
    // public ResponseEntity<Void> deleteUser(@PathVariable Integer id) {
    // try {
    // userService.deleteUser(id);
    // return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    // } catch (RuntimeException e) {
    // return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    // }
    // }
}
