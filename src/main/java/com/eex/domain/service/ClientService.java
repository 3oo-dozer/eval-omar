package com.eex.domain.service;

import com.eex.domain.entity.Client;
import com.eex.infrastructure.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClientService {
    private final ClientRepository clientRepository;

    @Transactional(readOnly = true)
    public Optional<Client> getClientById(Integer id) {
        return clientRepository.findById(id);
    }

    // public Client createClient(Client client) {
    // return clientRepository.save(client);
    // }

    // public Client updateClient(Integer id, Client clientDetails) {
    // Optional<Client> optionalClient = clientRepository.findById(id);
    // if (optionalClient.isPresent()) {
    // Client client = optionalClient.get();
    // if (clientDetails.getLegalEntity() != null) {
    // client.setLegalEntity(clientDetails.getLegalEntity());
    // }
    // return clientRepository.save(client);
    // }
    // throw new RuntimeException("Client introuvable avec l'id " + id);
    // }

    // public void deleteClient(Integer id) {
    // if (clientRepository.existsById(id)) {
    // clientRepository.deleteById(id);
    // } else {
    // throw new RuntimeException("Client introuvable avec l'id " + id);
    // }
    // }
}
