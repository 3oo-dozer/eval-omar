package com.eex.domain.service;

import com.eex.domain.entity.User;
import com.eex.infrastructure.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void getUserForClientIsScopedToClient() {
        User alice = new User(1, "Alice", "hash", 1);
        when(userRepository.findByIdAndClientId(1, 1)).thenReturn(Optional.of(alice));
        when(userRepository.findByIdAndClientId(1, 2)).thenReturn(Optional.empty());

        assertThat(userService.getUserForClient(1, 1)).contains(alice);
        assertThat(userService.getUserForClient(1, 2)).isEmpty();
    }

    @Test
    void listsUsersOfTheGivenClient() {
        when(userRepository.findByClientId(3)).thenReturn(List.of(new User(3, "Chloé", "hash", 3)));

        assertThat(userService.getUsersByClientId(3)).extracting(User::getUsername).containsExactly("Chloé");
    }
}
