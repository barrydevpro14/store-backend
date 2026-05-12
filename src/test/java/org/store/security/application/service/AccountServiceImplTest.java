package org.store.security.application.service;

import org.store.security.application.service.impl.AccountServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.store.common.exceptions.EntityException;
import org.store.common.exceptions.UniqueResourceException;
import org.store.security.application.dto.AccountRequest;
import org.store.security.domain.model.Account;
import org.store.security.domain.model.Role;
import org.store.security.domain.service.AccountDomainService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private AccountDomainService accountDomainService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AccountServiceImpl service;

    @Test
    void create_should_encode_password_and_delegate_to_domain() {
        AccountRequest request = new AccountRequest("john.doe", "S3cretPwd!");
        Role role = new Role();
        Account expected = new Account();

        when(accountDomainService.findByUsername("john.doe")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("S3cretPwd!")).thenReturn("hashed");
        when(accountDomainService.create(eq("john.doe"), eq("hashed"), eq(role))).thenReturn(expected);

        Account result = service.create(request, role);

        assertThat(result).isSameAs(expected);
    }

    @Test
    void should_throw_unique_resource_when_username_already_exists() {
        AccountRequest request = new AccountRequest("john.doe", "S3cretPwd!");
        when(accountDomainService.findByUsername("john.doe")).thenReturn(Optional.of(new Account()));

        assertThatThrownBy(() -> service.create(request, new Role()))
                .isInstanceOf(UniqueResourceException.class);

        verify(accountDomainService, never()).create(any(), any(), any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void should_return_account_when_findByUsername_exists() {
        Account account = new Account();
        when(accountDomainService.findByUsername("john.doe")).thenReturn(Optional.of(account));

        Account result = service.findByUsername("john.doe");

        assertThat(result).isSameAs(account);
    }

    @Test
    void should_throw_entity_exception_when_findByUsername_unknown() {
        when(accountDomainService.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByUsername("ghost"))
                .isInstanceOf(EntityException.class);
    }
}
