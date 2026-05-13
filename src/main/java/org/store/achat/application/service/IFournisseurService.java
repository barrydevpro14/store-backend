package org.store.achat.application.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.store.achat.application.dto.FournisseurRequest;
import org.store.achat.application.dto.FournisseurResponse;
import org.store.achat.domain.model.Fournisseur;

import java.util.UUID;

public interface IFournisseurService {

    /**
     * Création d'un fournisseur pour l'entreprise du caller.
     */
    FournisseurResponse create(FournisseurRequest fournisseurRequest);

    /**
     * Lecture interne par id (utilisée par d'autres agrégats).
     */
    Fournisseur findById(UUID id);

    /**
     * Lecture par id, scopée sur l'entreprise du caller.
     */
    FournisseurResponse findResponseById(UUID id);

    /**
     * Listing paginé des fournisseurs de l'entreprise du caller.
     */
    Page<FournisseurResponse> findAllByCurrentEntreprise(Pageable pageable);

    /**
     * Modification d'un fournisseur de l'entreprise du caller.
     */
    FournisseurResponse update(UUID id, FournisseurRequest fournisseurRequest);

    /**
     * Suppression d'un fournisseur de l'entreprise du caller.
     */
    void delete(UUID id);

    /**
     * Vérifie qu'un fournisseur appartient à l'entreprise du caller. Throw `ForbiddenException("fournisseur.notOwned")` sinon.
     */
    Fournisseur ensureBelongsToCurrentEntreprise(Fournisseur fournisseur);

    /**
     * Vérifie qu'aucun fournisseur de l'entreprise donnée ne porte déjà cette référence (skippé si reference null/blank). Throw `UniqueResourceException("fournisseur.reference.alreadyExists")` sinon.
     */
    void ensureReferenceAvailable(String reference, UUID entrepriseId);
}
