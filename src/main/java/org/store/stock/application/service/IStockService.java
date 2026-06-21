package org.store.stock.application.service;

import org.springframework.data.domain.Page;
import org.store.stock.application.dto.StockFilter;
import org.store.stock.application.dto.StockResponse;
import org.store.stock.application.dto.StockThresholdRequest;
import org.store.stock.application.dto.StockValuationResponse;

import java.util.UUID;

public interface IStockService {

    /**
     * Lecture d'un stock par id, scopée sur l'entreprise du caller (et sur son magasin propre s'il est employé).
     */
    StockResponse findResponseById(UUID id);

    /**
     * Liste paginée des stocks de l'entreprise du caller à partir d'un filter validé.
     * Le filter porte les critères (magasin, produit) + la pagination (page, size).
     */
    Page<StockResponse> findAllByCurrentEntreprise(StockFilter filter);

    /**
     * Liste paginée des stocks sous le seuil d'approvisionnement (quantiteDisponible &lt;= seuil
     * et seuil &gt; 0) pour le magasin ciblé.
     */
    Page<StockResponse> findBelowThresholdByCurrentEntreprise(StockFilter filter);

    /**
     * Met à jour le seuil d'approvisionnement d'un stock après vérification d'accès magasin.
     */
    StockResponse updateThreshold(UUID id, StockThresholdRequest stockThresholdRequest);

    /**
     * Nombre de stocks sous seuil pour le magasin ciblé, après vérification d'accès.
     */
    long countBelowThresholdByCurrentEntreprise(UUID magasinId);

    /**
     * Calcule la valorisation totale du stock d'un magasin (SUM(qty × prixAchatMoyen))
     * après vérification d'accès magasin.
     */
    StockValuationResponse computeValuation(UUID magasinId);
}
