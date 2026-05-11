package org.store.security.application.service;


import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.security.domain.model.Account;
import org.store.security.domain.model.Permissions;
import org.store.security.domain.model.Role;
import org.store.security.domain.repository.AccountRepository;

import java.util.Collection;
import java.util.Collections;


@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final AccountRepository accountRepository;

    public UserDetailsServiceImpl(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Compte introuvable : " + username));

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
        if (role == null || role.getPermissions() == null) {
            return Collections.emptyList();
        }
        return role.getPermissions().stream()
                .map(Permissions::getCode)
                .filter(code -> code != null && !code.isBlank())
                .map(SimpleGrantedAuthority::new)
                .toList();
    }
}
