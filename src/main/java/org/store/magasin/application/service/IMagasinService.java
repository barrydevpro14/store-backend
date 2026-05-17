package org.store.magasin.application.service;

import org.springframework.data.domain.Page;
import org.store.entreprise.domain.model.Entreprise;
import org.store.magasin.application.dto.MagasinFilter;
import org.store.magasin.application.dto.MagasinRequest;
import org.store.magasin.application.dto.MagasinResponse;
import org.store.magasin.domain.model.Magasin;

import java.util.UUID;

public interface IMagasinService {

    /**
     * Création interne (flux d'inscription propriétaire). Entreprise déjà connue.
     */
    Magasin create(MagasinRequest magasinRequest, Entreprise entreprise);

    /**
     * Création publique : magasin créé dans l'entreprise du caller (PROPRIETAIRE_ACCESS requis).
     */
    MagasinResponse create(MagasinRequest magasinRequest);

    /**
     * Lecture interne (utilisée par d'autres agrégats).
     */
    Magasin findById(UUID id);

    MagasinResponse findResponseById(UUID id);

    Page<MagasinResponse> findAllByCurrentEntreprise(MagasinFilter filter);

    MagasinResponse update(UUID id, MagasinRequest magasinRequest);

    MagasinResponse activate(UUID id);

    MagasinResponse deactivate(UUID id);

    /**
     * Vérifie qu'un magasin appartient à l'entreprise du caller. Throw `ForbiddenException("magasin.notOwned")` sinon.
     * Check strict "scope entreprise" — convient aux opérations propriétaire-only.
     */
    Magasin ensureBelongsToCurrentEntreprise(Magasin magasin);

    /**
     * Vérifie qu'un magasin est accessible par le caller :
     * - Si propriétaire : check entreprise (délègue à {@link #ensureBelongsToCurrentEntreprise}).
     * - Sinon (manager d'un magasin) : check `magasin.id == currentUser.magasinId`.
     * Throw `ForbiddenException("magasin.notOwned")` sinon.
     */
    Magasin ensureAccessibleByCurrentUser(Magasin magasin);
}
