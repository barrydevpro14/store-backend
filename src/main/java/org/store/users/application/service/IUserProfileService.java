package org.store.users.application.service;

import org.store.security.application.dto.ChangePasswordRequest;
import org.store.users.application.dto.UserProfileResponse;
import org.store.users.application.dto.UserProfileUpdateRequest;

public interface IUserProfileService {

    /** Retourne le profil de l'utilisateur connecté. */
    UserProfileResponse getCurrentProfile();

    /** Met à jour les champs Person du profil connecté (nom, prenom, email, telephone, adresse). */
    UserProfileResponse updateCurrentProfile(UserProfileUpdateRequest request);

    /** Change le mot de passe du compte connecté après vérification de l'ancien. */
    void changePassword(ChangePasswordRequest request);
}
