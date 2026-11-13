package com.eex.application.dto;

import com.eex.domain.entity.User;

/** Représentation publique d'un utilisateur : le hash du mot de passe n'est jamais exposé. */
public record UserResponse(Integer id, String username, Integer clientId) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getClientId());
    }
}
