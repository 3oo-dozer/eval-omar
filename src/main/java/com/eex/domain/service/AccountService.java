package com.eex.domain.service;

import com.eex.domain.entity.Account;
import com.eex.infrastructure.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Toutes les opérations sont restreintes au client propriétaire : un compte
 * d'un autre client
 * est traité comme inexistant.
 */
@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public List<Account> getAccountsByClientId(Integer clientId) {
        return accountRepository.findByClientId(clientId);
    }

    @Transactional(readOnly = true)
    public Optional<Account> getAccountForClient(Integer id, Integer clientId) {
        return accountRepository.findByIdAndClientId(id, clientId);
    }

    @Transactional(readOnly = true)
    public List<Account> getAccountsByNameForClient(String name, Integer clientId) {
        return accountRepository.findByClientIdAndName(clientId, name);
    }

    /**
     * Renomme un compte du client. Le montant et le client ne sont jamais modifiés.
     */
    @Transactional
    public Optional<Account> renameAccount(Integer id, Integer clientId, String newName) {
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("Le nom du compte est obligatoire");
        }
        return accountRepository.findByIdAndClientId(id, clientId)
                .map(account -> {
                    account.setName(newName.trim());
                    return accountRepository.save(account);
                });
    }

    // public List<Account> getAllAccounts() {
    // return accountRepository.findAll();
    // }

    // public Optional<Account> getAccountById(Integer id) {
    // return accountRepository.findById(id);
    // }

    // public void deleteAccount(Integer id) {
    // if (accountRepository.existsById(id)) {
    // accountRepository.deleteById(id);
    // } else {
    // throw new RuntimeException("Compte introuvable avec l'id " + id);
    // }
    // }
}
