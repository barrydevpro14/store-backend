package org.store.security.application.service;

import org.store.common.dto.UserSummaryResponse;
import org.store.security.application.dto.AccountRequest;
import org.store.security.domain.model.Account;
import org.store.security.domain.model.Role;

import java.util.Optional;
import java.util.UUID;

public interface IAccountService {

    Account create(AccountRequest accountRequest, Role role);

    Account findByUsername(String username);

    /** Lecture safe d'un Account par id (retourne empty si introuvable, sans throw). */
    Optional<Account> findOptionalById(UUID accountId);

    /** Résout un createdBy (= accountId stringifié) en `UserSummaryResponse(id, nomComplet)`. Retourne empty si accountId null/invalide ou Account/user introuvable. */
    Optional<UserSummaryResponse> findUserSummaryByAccountId(String accountIdString);

    /** Active ou désactive un Account (bloque/autorise le login JWT). */
    Account setEnabled(Account account, boolean enabled);

    /** Change le mot de passe : vérifie l'ancien avec PasswordEncoder.matches puis hash et persiste le nouveau. Throw `BadArgumentException("account.currentPassword.invalid")` si l'ancien ne correspond pas. */
    Account changePassword(Account account, String currentPassword, String newPassword);
}
