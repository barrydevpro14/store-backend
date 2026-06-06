package org.store.security.application.dto;

import org.store.security.domain.model.Account;
import org.store.users.application.dto.UserResponse;

import java.util.UUID;

public record AccountResponse(
        UUID id,
        String username,
        boolean enabled,
        boolean locked,
        String role,
        UserResponse user
) {
    public AccountResponse(Account account) {
        this(
                account.getId(),
                account.getUsername(),
                account.isEnabled(),
                account.isLocked(),
                account.getRole().getLibelle(),
                new UserResponse(account.getUser())
        );
    }
}
