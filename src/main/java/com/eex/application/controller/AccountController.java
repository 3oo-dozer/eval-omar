package com.eex.application.controller;

import com.eex.application.dto.AccountNameUpdateRequest;
import com.eex.application.dto.AccountResponse;
import com.eex.domain.service.AccountService;
import com.eex.infrastructure.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {
        private final AccountService accountService;

        /** Les comptes du client connecté. */
        @GetMapping
        public List<AccountResponse> getMyAccounts(@AuthenticationPrincipal AuthenticatedUser me) {
                return accountService.getAccountsByClientId(me.clientId()).stream()
                                .map(AccountResponse::from)
                                .toList();
        }

        @GetMapping("/{id}")
        public ResponseEntity<AccountResponse> getAccountById(@PathVariable Integer id,
                        @AuthenticationPrincipal AuthenticatedUser me) {
                return accountService.getAccountForClient(id, me.clientId())
                                .map(AccountResponse::from)
                                .map(ResponseEntity::ok)
                                .orElseGet(() -> ResponseEntity.notFound().build());
        }

        @GetMapping("/search")
        public List<AccountResponse> getAccountsByName(@RequestParam String name,
                        @AuthenticationPrincipal AuthenticatedUser me) {
                return accountService.getAccountsByNameForClient(name, me.clientId()).stream()
                                .map(AccountResponse::from)
                                .toList();
        }

        /** Seul le nom du compte peut être modifié (le montant ne l'est jamais). */
        @RequestMapping(value = "/{id}", method = { RequestMethod.PUT, RequestMethod.PATCH })
        public ResponseEntity<AccountResponse> renameAccount(@PathVariable Integer id,
                        @Valid @RequestBody AccountNameUpdateRequest request,
                        @AuthenticationPrincipal AuthenticatedUser me) {
                return accountService.renameAccount(id, me.clientId(), request.name())
                                .map(AccountResponse::from)
                                .map(ResponseEntity::ok)
                                .orElseGet(() -> ResponseEntity.notFound().build());
        }

        // @PostMapping
        // public ResponseEntity<Account> createAccount(@RequestBody Account account) {
        // Account createdAccount = accountService.createAccount(account);
        // return new ResponseEntity<>(createdAccount, HttpStatus.CREATED);
        // }

        // @PutMapping("/{id}")
        // public ResponseEntity<Account> updateAccount(@PathVariable Integer id,
        // @RequestBody Account accountDetails) {
        // try {
        // Account updatedAccount = accountService.updateAccount(id, accountDetails);
        // return new ResponseEntity<>(updatedAccount, HttpStatus.OK);
        // } catch (RuntimeException e) {
        // return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        // }
        // }

        // @DeleteMapping("/{id}")
        // public ResponseEntity<Void> deleteAccount(@PathVariable Integer id) {
        // try {
        // accountService.deleteAccount(id);
        // return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        // } catch (RuntimeException e) {
        // return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        // }
        // }
}
