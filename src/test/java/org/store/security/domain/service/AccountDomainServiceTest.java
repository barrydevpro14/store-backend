package org.store.security.domain.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.security.domain.model.Account;
import org.store.security.domain.model.Role;
import org.store.security.domain.repository.AccountRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountDomainServiceTest {

    @Mock
    private AccountRepository repository;

    @InjectMocks
    private AccountDomainService service;

    @Test
    void create_should_construct_enabled_unlocked_account_with_hashed_password_and_role() {
        Role role = new Role();
        when(repository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        Account result = service.create("john.doe", "hashed-value", role);

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(repository).save(captor.capture());
        Account saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("john.doe");
        assertThat(saved.getPassword()).isEqualTo("hashed-value");
        assertThat(saved.isEnabled()).isTrue();
        assertThat(saved.isLocked()).isFalse();
        assertThat(saved.getRole()).isSameAs(role);
        assertThat(result).isSameAs(saved);
    }

    @Test
    void findByUsername_should_delegate_to_repository() {
        Account account = new Account();
        when(repository.findByUsername("john.doe")).thenReturn(Optional.of(account));

        assertThat(service.findByUsername("john.doe")).contains(account);
    }
}
