package com.eex.infrastructure.security;

/**
 * Principal placé dans le SecurityContext après validation du JWT.
 * Le clientId sert à restreindre toutes les lectures/écritures aux données du client connecté.
 */
public record AuthenticatedUser(Integer userId, String username, Integer clientId) {
}
