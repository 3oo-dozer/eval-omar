package com.eex.domain.service;

import com.eex.domain.entity.Account;
import com.eex.infrastructure.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

    private static Account account() {
        return new Account(1, "AMAZON", 1000.0, 1, null);
    }

    @Test
    void listsOnlyAccountsOfTheGivenClient() {
        when(accountRepository.findByClientId(1)).thenReturn(List.of(account()));

        assertThat(accountService.getAccountsByClientId(1)).extracting(Account::getName).containsExactly("AMAZON");
        verify(accountRepository).findByClientId(1);
    }

    @Test
    void getAccountIsScopedToClient() {
        when(accountRepository.findByIdAndClientId(1, 2)).thenReturn(Optional.empty());

        assertThat(accountService.getAccountForClient(1, 2)).isEmpty();
    }

    @Test
    void searchByNameIsScopedToClient() {
        when(accountRepository.findByClientIdAndName(1, "AMAZON")).thenReturn(List.of(account()));

        assertThat(accountService.getAccountsByNameForClient("AMAZON", 1)).hasSize(1);
    }

    @Test
    void renameChangesOnlyTheName() {
        Account existing = account();
        when(accountRepository.findByIdAndClientId(1, 1)).thenReturn(Optional.of(existing));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        Account renamed = accountService.renameAccount(1, 1, "  Nouveau nom  ").orElseThrow();

        assertThat(renamed.getName()).isEqualTo("Nouveau nom");
        assertThat(renamed.getAmount()).isEqualTo(1000.0);
        assertThat(renamed.getClientId()).isEqualTo(1);
        assertThat(renamed.getId()).isEqualTo(1);
    }

    @Test
    void renameOfAnotherClientsAccountDoesNothing() {
        when(accountRepository.findByIdAndClientId(1, 2)).thenReturn(Optional.empty());

        assertThat(accountService.renameAccount(1, 2, "Piratage")).isEmpty();
        verify(accountRepository, never()).save(any());
    }

    @Test
    void renameWithBlankNameIsRejected() {
        assertThatThrownBy(() -> accountService.renameAccount(1, 1, " "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> accountService.renameAccount(1, 1, null))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(accountRepository);
    }
}
