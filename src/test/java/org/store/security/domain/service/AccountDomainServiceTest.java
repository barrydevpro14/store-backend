package org.store.security.domain.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.store.security.domain.model.Account;
import org.store.security.domain.model.Role;
import org.store.security.domain.repository.AccountRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    @Test
    void existsByUsername_should_return_true_when_repository_returns_true() {
        when(repository.existsByUsername("john.doe")).thenReturn(true);

        assertThat(service.existsByUsername("john.doe")).isTrue();
    }

    @Test
    void existsByUsername_should_return_false_when_repository_returns_false() {
        when(repository.existsByUsername("unknown")).thenReturn(false);

        assertThat(service.existsByUsername("unknown")).isFalse();
    }

    @Test
    void findAllByRoleLibelle_should_delegate_to_repository() {
        Pageable pageable = PageRequest.of(0, 10);
        Account account = new Account();
        Page<Account> page = new PageImpl<>(List.of(account), pageable, 1);

        when(repository.findAllByRoleLibelle("ADMIN", pageable)).thenReturn(page);

        Page<Account> result = service.findAllByRoleLibelle("ADMIN", pageable);

        assertThat(result.getContent()).containsExactly(account);
        verify(repository).findAllByRoleLibelle("ADMIN", pageable);
    }

    @Test
    void findOptionalById_should_return_optional_from_repository() {
        UUID id = UUID.randomUUID();
        Account account = new Account();
        when(repository.findById(id)).thenReturn(Optional.of(account));

        assertThat(service.findOptionalById(id)).contains(account);
    }

    @Test
    void findOptionalById_should_return_empty_when_not_found() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThat(service.findOptionalById(id)).isEmpty();
    }

    @Test
    void setEnabled_should_update_flag_and_save() {
        Account account = new Account();
        account.setEnabled(true);
        when(repository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        Account result = service.setEnabled(account, false);

        assertThat(result.isEnabled()).isFalse();
        verify(repository).save(account);
    }

    @Test
    void changePassword_should_update_password_and_save() {
        Account account = new Account();
        account.setPassword("old-hash");
        when(repository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        Account result = service.changePassword(account, "new-hash");

        assertThat(result.getPassword()).isEqualTo("new-hash");
        verify(repository).save(account);
    }
}
