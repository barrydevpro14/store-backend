package org.store.entreprise.application.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import org.store.common.dto.ImageDownloadResponse;
import org.store.entreprise.application.dto.EntrepriseRequest;
import org.store.entreprise.application.dto.EntrepriseResponse;
import org.store.entreprise.domain.model.Entreprise;
import org.store.users.domain.model.Proprietaire;

import java.util.UUID;

public interface IEntrepriseService {

    /**
     * Création interne (flux d'inscription propriétaire). Proprietaire déjà connu.
     */
    Entreprise create(EntrepriseRequest entrepriseRequest, Proprietaire proprietaire);

    /**
     * Lecture interne (utilisée par d'autres agrégats).
     */
    Entreprise findById(UUID id);

    /**
     * Lecture par le propriétaire de sa propre entreprise.
     */
    EntrepriseResponse findCurrentUserEntreprise();

    /**
     * Modification par le propriétaire des infos de sa propre entreprise.
     */
    EntrepriseResponse updateCurrentUserEntreprise(EntrepriseRequest entrepriseRequest);

    /**
     * Listing paginé de toutes les entreprises (ADMIN).
     */
    Page<EntrepriseResponse> findAll(Pageable pageable);

    /**
     * Lecture d'une entreprise par id (ADMIN).
     */
    EntrepriseResponse findResponseById(UUID id);

    /**
     * Activation d'une entreprise (ADMIN).
     */
    EntrepriseResponse activate(UUID id);

    /**
     * Désactivation d'une entreprise (ADMIN, soft delete).
     */
    EntrepriseResponse deactivate(UUID id);

    /**
     * Vérifie qu'une entreprise appartient au caller (propriétaire). Throw `ForbiddenException("entreprise.notOwned")` sinon.
     */
    Entreprise ensureBelongsToCurrentUser(Entreprise entreprise);

    /** Upload (ou remplace) le logo de l'entreprise du propriétaire connecté. */
    EntrepriseResponse uploadCurrentUserLogo(MultipartFile file);

    /** Télécharge le logo de l'entreprise du propriétaire connecté. Throw `EntityException("entreprise.logo.notFound")` si absent. */
    ImageDownloadResponse getCurrentUserLogo();

    /** Supprime le logo de l'entreprise du propriétaire connecté (idempotent). */
    void deleteCurrentUserLogo();
}
