package org.store.security.application.dto;

import org.store.common.tools.DateHelper;
import org.store.security.domain.model.Account;
import org.store.users.domain.model.Utilisateur;

import java.util.UUID;

public record AdminAccountResponse(
        UUID id,
        String username,
        String nom,
        String prenom,
        String email,
        String telephone,
        boolean enabled,
        boolean locked,
        boolean systeme,
        String createdAt
) {
    public AdminAccountResponse(Account account) {
        this(account, account.getUser());
    }

    public AdminAccountResponse(Account account, Utilisateur utilisateur) {
        this(
                account.getId(),
                account.getUsername(),
                utilisateur != null ? utilisateur.getNom() : null,
                utilisateur != null ? utilisateur.getPrenom() : null,
                utilisateur != null ? utilisateur.getEmail() : null,
                utilisateur != null ? utilisateur.getTelephone() : null,
                account.isEnabled(),
                account.isLocked(),
                account.isSysteme(),
                DateHelper.format(account.getCreatedAt())
        );
    }
}
