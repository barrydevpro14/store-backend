package org.store.produit.application.service;

import org.springframework.data.domain.Page;
import org.store.produit.application.dto.UniteMesureFilter;
import org.store.produit.application.dto.UniteMesureRequest;
import org.store.produit.application.dto.UniteMesureResponse;
import org.store.produit.application.dto.UniteMesureSummaryResponse;
import org.store.produit.domain.model.UniteMesure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IUniteMesureService {

    /**
     * Création d'une unité de mesure (ADMIN uniquement).
     */
    UniteMesureResponse create(UniteMesureRequest request);

    /**
     * Lecture interne par id (utilisée par d'autres agrégats).
     */
    UniteMesure findById(UUID id);

    /**
     * Lecture par id en DTO.
     */
    UniteMesureResponse findResponseById(UUID id);

    /**
     * Listing paginé et filtré des unités de mesure.
     */
    Page<UniteMesureResponse> findAll(UniteMesureFilter filter);

    /**
     * Liste complète ordonnée par libellé, pour les sélecteurs produit.
     */
    List<UniteMesureSummaryResponse> listAll();

    /**
     * Modification d'une unité de mesure (ADMIN uniquement).
     */
    UniteMesureResponse update(UUID id, UniteMesureRequest request);

    /**
     * Suppression d'une unité de mesure (ADMIN uniquement).
     */
    void delete(UUID id);

    /**
     * Recherche une unité de mesure par son code technique (ex: "PIECE", "KG").
     */
    UniteMesure findByCode(String code);

    /**
     * Recherche une unité de mesure par son code technique sans lever d'exception si absente.
     */
    Optional<UniteMesure> findByCodeOptional(String code);

    /**
     * Vérifie que le code n'est pas déjà utilisé. Throw {@code UniqueResourceException("uniteMesure.code.alreadyExists")} sinon.
     */
    void ensureCodeAvailable(String code);
}
