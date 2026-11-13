package com.eex.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Seul le nom d'un compte est modifiable. Tout autre champ envoyé (amount, clientId, id...)
 * est ignoré lors de la désérialisation et ne peut donc pas être modifié.
 */
public record AccountNameUpdateRequest(@NotBlank @Size(max = 100) String name) {
}
