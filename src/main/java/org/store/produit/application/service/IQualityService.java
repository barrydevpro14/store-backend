package org.store.produit.application.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.store.produit.application.dto.QualityFilter;
import org.store.produit.application.dto.QualityRequest;
import org.store.produit.application.dto.QualityResponse;
import org.store.produit.application.dto.QualitySummaryResponse;
import org.store.produit.domain.model.Quality;

import java.util.Optional;
import java.util.UUID;

public interface IQualityService {

    /**
     * Création d'une qualité pour l'entreprise du caller.
     */
    QualityResponse create(QualityRequest qualityRequest);

    /**
     * Lecture interne par id (utilisée par d'autres agrégats).
     */
    Quality findById(UUID id);

    /**
     * Lecture par id, scopée sur l'entreprise du caller.
     */
    QualityResponse findResponseById(UUID id);

    /**
     * Listing paginé + filtré des qualités de l'entreprise du caller.
     */
    Page<QualityResponse> findAll(QualityFilter filter);

    /**
     * Modification d'une qualité de l'entreprise du caller.
     */
    QualityResponse update(UUID id, QualityRequest qualityRequest);

    /**
     * Suppression d'une qualité de l'entreprise du caller.
     */
    void delete(UUID id);

    /**
     * Vérifie qu'une qualité appartient à l'entreprise du caller. Throw `ForbiddenException("quality.notOwned")` sinon.
     */
    Quality ensureBelongsToCurrentEntreprise(Quality quality);

    /**
     * Vérifie qu'aucune qualité de l'entreprise donnée ne porte déjà ce libellé. Throw `UniqueResourceException("quality.libelle.alreadyExists")` sinon.
     */
    void ensureLibelleAvailable(String libelle, UUID entrepriseId);

    /**
     * Recherche une qualité par son libellé pour l'entreprise du caller.
     */
    Optional<Quality> findByLibelle(String libelle);

    /**
     * Recherche paginée de qualités pour les sélecteurs.
     */
    Page<QualitySummaryResponse> search(String q, Pageable pageable);
}
