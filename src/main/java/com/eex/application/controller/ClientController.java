package com.eex.application.controller;

import com.eex.application.dto.ClientResponse;
import com.eex.domain.service.ClientService;
import com.eex.infrastructure.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientController {
    private final ClientService clientService;

    /** Le client de l'utilisateur connecté. */
    @GetMapping("/me")
    public ResponseEntity<ClientResponse> getMyClient(@AuthenticationPrincipal AuthenticatedUser me) {
        return clientService.getClientById(me.clientId())
                .map(ClientResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Accessible uniquement pour son propre client ; tout autre id renvoie 404. */
    @GetMapping("/{id}")
    public ResponseEntity<ClientResponse> getClientById(@PathVariable Integer id,
            @AuthenticationPrincipal AuthenticatedUser me) {
        if (!id.equals(me.clientId())) {
            return ResponseEntity.notFound().build();
        }
        return getMyClient(me);
    }

    // @PostMapping
    // public ResponseEntity<Client> createClient(@RequestBody Client client) {
    // Client createdClient = clientService.createClient(client);
    // return new ResponseEntity<>(createdClient, HttpStatus.CREATED);
    // }

    // @PutMapping("/{id}")
    // public ResponseEntity<Client> updateClient(@PathVariable Integer id,
    // @RequestBody Client clientDetails) {
    // try {
    // Client updatedClient = clientService.updateClient(id, clientDetails);
    // return new ResponseEntity<>(updatedClient, HttpStatus.OK);
    // } catch (RuntimeException e) {
    // return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    // }
    // }

    // @DeleteMapping("/{id}")
    // public ResponseEntity<Void> deleteClient(@PathVariable Integer id) {
    // try {
    // clientService.deleteClient(id);
    // return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    // } catch (RuntimeException e) {
    // return new ResponseEntity<>(HttpStatus.NOT_FOUND);

    // }
    // }
}
