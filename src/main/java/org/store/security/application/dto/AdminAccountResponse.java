package org.store.security.application.dto;

import org.store.common.tools.DateHelper;
import org.store.security.domain.model.Account;

import java.util.UUID;

public record AdminAccountResponse(
        UUID id,
        String username,
        boolean enabled,
        boolean locked,
        String createdAt
) {
    public AdminAccountResponse(Account account) {
        this(
                account.getId(),
                account.getUsername(),
                account.isEnabled(),
                account.isLocked(),
                DateHelper.format(account.getCreatedAt())
        );
    }
}
