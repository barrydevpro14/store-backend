package org.store.users.application.service;

import org.springframework.web.multipart.MultipartFile;
import org.store.common.dto.ImageDownloadResponse;
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

    /** Upload (ou remplace) la photo de profil de l'utilisateur connecté. */
    UserProfileResponse uploadPhoto(MultipartFile file);

    /** Télécharge la photo de profil de l'utilisateur connecté. Throw `EntityException("user.photo.notFound")` si absent. */
    ImageDownloadResponse getPhoto();

    /** Supprime la photo de profil de l'utilisateur connecté. */
    void deletePhoto();
}
