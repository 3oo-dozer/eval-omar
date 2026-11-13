package com.eex.domain.service;

import com.eex.domain.entity.Client;
import com.eex.infrastructure.repository.ClientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private ClientService clientService;

    @Test
    void returnsClientById() {
        Client client = new Client(1, true);
        when(clientRepository.findById(1)).thenReturn(Optional.of(client));

        assertThat(clientService.getClientById(1)).contains(client);
    }
}
