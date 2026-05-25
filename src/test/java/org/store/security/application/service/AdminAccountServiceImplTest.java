package org.store.security.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.service.ValidatorService;
import org.store.security.application.dto.AdminAccountRequest;
import org.store.security.application.dto.AdminAccountResponse;
import org.store.security.application.service.impl.AdminAccountServiceImpl;
import org.store.security.domain.model.Account;
import org.store.security.domain.model.Role;
import org.store.security.domain.service.AccountDomainService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAccountServiceImplTest {

    @Mock
    private AccountDomainService accountDomainService;

    @Mock
    private IRoleService roleService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ValidatorService validatorService;

    @InjectMocks
    private AdminAccountServiceImpl service;

    private Account adminAccount;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        adminRole = new Role();
        adminRole.setId(UUID.randomUUID());
        adminRole.setLibelle("ADMIN");

        adminAccount = new Account();
        adminAccount.setId(UUID.randomUUID());
        adminAccount.setUsername("admin-user");
        adminAccount.setEnabled(true);
        adminAccount.setLocked(false);
        adminAccount.setRole(adminRole);
    }

    @Test
    void findAll_should_delegate_to_domain_service_and_map_responses() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Account> accountPage = new PageImpl<>(List.of(adminAccount), pageable, 1);

        when(accountDomainService.findAllByRoleLibelle("ADMIN", pageable)).thenReturn(accountPage);

        Page<AdminAccountResponse> result = service.findAll(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).username()).isEqualTo("admin-user");
        assertThat(result.getContent().get(0).enabled()).isTrue();

        verify(accountDomainService).findAllByRoleLibelle("ADMIN", pageable);
    }

    @Test
    void findAll_should_return_empty_page_when_no_admins() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Account> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(accountDomainService.findAllByRoleLibelle("ADMIN", pageable)).thenReturn(emptyPage);

        Page<AdminAccountResponse> result = service.findAll(pageable);

        assertThat(result).isEmpty();
    }

    @Test
    void create_should_validate_then_create_account_and_return_response() {
        AdminAccountRequest request = new AdminAccountRequest("new-admin", "securePass123");

        when(accountDomainService.existsByUsername("new-admin")).thenReturn(false);
        when(roleService.findByLibelle("ADMIN")).thenReturn(adminRole);
        when(passwordEncoder.encode("securePass123")).thenReturn("hashed-pass");

        Account created = new Account();
        created.setId(UUID.randomUUID());
        created.setUsername("new-admin");
        created.setEnabled(true);
        created.setLocked(false);

        when(accountDomainService.create("new-admin", "hashed-pass", adminRole)).thenReturn(created);

        AdminAccountResponse response = service.create(request);

        assertThat(response.username()).isEqualTo("new-admin");
        assertThat(response.enabled()).isTrue();

        verify(validatorService).validate(request);
        verify(accountDomainService).existsByUsername("new-admin");
        verify(roleService).findByLibelle("ADMIN");
        verify(passwordEncoder).encode("securePass123");
        verify(accountDomainService).create("new-admin", "hashed-pass", adminRole);
    }

    @Test
    void create_should_throw_BadArgumentException_when_username_already_exists() {
        AdminAccountRequest request = new AdminAccountRequest("existing-admin", "securePass123");

        when(accountDomainService.existsByUsername("existing-admin")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadArgumentException.class);

        verify(validatorService).validate(request);
        verify(accountDomainService).existsByUsername("existing-admin");
        verify(roleService, never()).findByLibelle(anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(accountDomainService, never()).create(anyString(), anyString(), any());
    }

    @Test
    void setEnabled_should_find_account_delegate_and_return_response() {
        UUID accountId = adminAccount.getId();

        Account disabledAccount = new Account();
        disabledAccount.setId(accountId);
        disabledAccount.setUsername("admin-user");
        disabledAccount.setEnabled(false);
        disabledAccount.setLocked(false);

        when(accountDomainService.findById(accountId)).thenReturn(adminAccount);
        when(accountDomainService.setEnabled(adminAccount, false)).thenReturn(disabledAccount);

        AdminAccountResponse response = service.setEnabled(accountId, false);

        assertThat(response.enabled()).isFalse();
        assertThat(response.username()).isEqualTo("admin-user");

        verify(accountDomainService).findById(accountId);
        verify(accountDomainService).setEnabled(adminAccount, false);
    }

    @Test
    void setEnabled_should_enable_account_and_return_response() {
        UUID accountId = adminAccount.getId();
        adminAccount.setEnabled(false);

        Account enabledAccount = new Account();
        enabledAccount.setId(accountId);
        enabledAccount.setUsername("admin-user");
        enabledAccount.setEnabled(true);
        enabledAccount.setLocked(false);

        when(accountDomainService.findById(accountId)).thenReturn(adminAccount);
        when(accountDomainService.setEnabled(adminAccount, true)).thenReturn(enabledAccount);

        AdminAccountResponse response = service.setEnabled(accountId, true);

        assertThat(response.enabled()).isTrue();
    }
}
