package org.store.users.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.exceptions.EntityException;
import org.store.common.service.ValidatorService;
import org.store.security.application.dto.ChangePasswordRequest;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.IAccountService;
import org.store.security.application.service.ICurrentUserService;
import org.store.security.domain.model.Account;
import org.store.users.application.dto.UserProfileResponse;
import org.store.users.application.dto.UserProfileUpdateRequest;
import org.store.users.application.service.IUserProfileService;
import org.store.users.domain.model.Utilisateur;
import org.store.users.domain.service.UtilisateurDomainService;

/**
 * Self-service profil utilisateur : lecture, mise a jour des infos personnelles
 * et changement de mot de passe (avec verification de l'ancien). Toutes les
 * operations portent sur l'utilisateur connecte (ICurrentUserService).
 */
@Service
@Transactional(readOnly = true)
public class UserProfileServiceImpl implements IUserProfileService {

    private final UtilisateurDomainService utilisateurDomainService;
    private final IAccountService accountService;
    private final ICurrentUserService currentUserService;
    private final ValidatorService validatorService;

    public UserProfileServiceImpl(UtilisateurDomainService utilisateurDomainService,
                                  IAccountService accountService,
                                  ICurrentUserService currentUserService,
                                  ValidatorService validatorService) {
        this.utilisateurDomainService = utilisateurDomainService;
        this.accountService = accountService;
        this.currentUserService = currentUserService;
        this.validatorService = validatorService;
    }

    @Override
    public UserProfileResponse getCurrentProfile() {
        return new UserProfileResponse(findCurrentUser());
    }

    @Override
    @Transactional
    public UserProfileResponse updateCurrentProfile(UserProfileUpdateRequest request) {
        validatorService.validate(request);
        Utilisateur updated = utilisateurDomainService.update(findCurrentUser(), request);
        return new UserProfileResponse(updated);
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        validatorService.validate(request);
        Account account = findCurrentUser().getAccount();
        accountService.changePassword(account, request.currentPassword(), request.newPassword());
    }

    /** Resout l'Utilisateur courant via userId du principal. Throw si l'utilisateur n'est plus en BDD (compte supprime mais JWT encore valide). */
    public Utilisateur findCurrentUser() {
        UserPrincipal currentUser = currentUserService.getCurrent();
        return utilisateurDomainService.findById(currentUser.userId());
    }
}
