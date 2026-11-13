package com.eex.application.dto;

import com.eex.domain.entity.Client;

public record ClientResponse(Integer id, Boolean legalEntity) {
    public static ClientResponse from(Client client) {
        return new ClientResponse(client.getId(), client.getLegalEntity());
    }
}
