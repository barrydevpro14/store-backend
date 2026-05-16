package org.store.security.application.service.impl;

import org.store.security.application.service.*;


import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.i18n.IMessageSourceService;
import org.store.security.domain.model.Account;
import org.store.security.domain.model.Role;
import org.store.security.domain.service.AccountDomainService;

import java.util.Collection;
import java.util.Collections;


@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final AccountDomainService accountDomainService;
    private final IPermissionsService permissionsService;
    private final IMessageSourceService messageSourceService;

    public UserDetailsServiceImpl(AccountDomainService accountDomainService,
                                  IPermissionsService permissionsService,
                                  IMessageSourceService messageSourceService) {
        this.accountDomainService = accountDomainService;
        this.permissionsService = permissionsService;
        this.messageSourceService = messageSourceService;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = accountDomainService.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        messageSourceService.getMessage("account.notFound", new Object[]{username})
                ));

        return User.builder()
                .username(account.getUsername())
                .password(account.getPassword())
                .disabled(!account.isEnabled())
                .accountLocked(account.isLocked())
                .authorities(extractAuthorities(account))
                .build();
    }

    private Collection<? extends GrantedAuthority> extractAuthorities(Account account) {
        Role role = account.getRole();
        if (role == null) {
            return Collections.emptyList();
        }

        return permissionsService.findAllByRoleId(role.getId()).stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
    }
}
