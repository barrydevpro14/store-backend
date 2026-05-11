package org.store.security.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.store.common.exceptions.UniqueResourceException;
import org.store.security.application.dto.AccountRequest;
import org.store.security.domain.model.Account;
import org.store.security.domain.model.Role;
import org.store.security.domain.repository.AccountRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AccountServiceImpl service;

    @Test
    void should_create_account_with_encoded_password_when_username_available() {
        AccountRequest request = new AccountRequest("john.doe", "S3cretPwd!");
        Role role = new Role();

        when(accountRepository.findByUsername("john.doe")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("S3cretPwd!")).thenReturn("hashed");
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        Account result = service.create(request, role);

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        Account saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("john.doe");
        assertThat(saved.getPassword()).isEqualTo("hashed");
        assertThat(saved.isEnabled()).isTrue();
        assertThat(saved.isLocked()).isFalse();
        assertThat(saved.getRole()).isSameAs(role);
        assertThat(result).isSameAs(saved);
    }

    @Test
    void should_throw_unique_resource_when_username_already_exists() {
        AccountRequest request = new AccountRequest("john.doe", "S3cretPwd!");
        when(accountRepository.findByUsername("john.doe")).thenReturn(Optional.of(new Account()));

        assertThatThrownBy(() -> service.create(request, new Role()))
                .isInstanceOf(UniqueResourceException.class);

        verify(accountRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }
}
