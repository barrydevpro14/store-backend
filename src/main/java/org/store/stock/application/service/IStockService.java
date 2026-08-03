package org.store.stock.application.service;

import org.springframework.data.domain.Page;
import org.store.magasin.domain.model.Magasin;
import org.store.produit.domain.model.ProductFournisseur;
import org.store.stock.domain.model.EntreeStock;
import org.store.stock.application.dto.BelowThresholdFilter;
import org.store.stock.application.dto.StockEntryContext;
import org.store.stock.application.dto.StockFilter;
import org.store.stock.application.dto.StockResponse;
import org.store.stock.application.dto.StockThresholdRequest;
import org.store.stock.application.dto.StockValuationResponse;
import org.store.stock.domain.model.Stock;

import java.util.List;
import java.util.Optional;
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
    Page<StockResponse> findBelowThresholdByCurrentEntreprise(BelowThresholdFilter filter);

    /**
     * Met à jour le seuil d'approvisionnement d'un stock après vérification d'accès magasin.
     */
    StockResponse updateThreshold(UUID id, StockThresholdRequest stockThresholdRequest);

    /**
     * Nombre de stocks sous seuil pour le magasin ciblé, après vérification d'accès.
     */
    long countBelowThresholdByCurrentEntreprise(UUID magasinId);

    /** Nombre de stocks sous seuil d'approvisionnement pour toute l'entreprise (KPI owner). */
    long countBelowThresholdByEntreprise(UUID entrepriseId);

    /**
     * Calcule la valorisation totale du stock d'un magasin (SUM(qty × prixAchatMoyen))
     * après vérification d'accès magasin.
     */
    StockValuationResponse computeValuation(UUID magasinId);

    /**
     * Surcharge pour un magasin déjà résolu — évite un aller-retour BDD supplémentaire.
     * Vérifie tout de même l'accès via {@code ensureAccessibleByCurrentUser}.
     */
    StockValuationResponse computeValuation(Magasin magasin);

    /**
     * Crée ou met à jour le stock agrégé (upsert PMP) lors d'une entrée.
     * Réservé à la coordination interne au domaine stock.
     */
    Stock createOrUpdateEntry(StockEntryContext context);

    /**
     * Retourne le stock agrégé pour une paire (magasin, productFournisseur), sans vérification d'accès.
     * Réservé à la coordination interne au domaine stock.
     */
    Optional<Stock> findByMagasinAndProductFournisseur(UUID magasinId, UUID productFournisseurId);

    Stock findById(UUID id);

    /**
     * Retourne le stock existant pour la paire (magasin, productFournisseur), ou en crée un vierge persisté
     * (qty=0, pmp=0). Réservé à la coordination interne — ne vérifie pas les droits d'accès.
     */
    Stock findOrCreate(Magasin magasin, ProductFournisseur productFournisseur);

    /**
     * Recalcule {@code quantiteDisponible} et {@code prixAchatMoyen} du stock depuis les lots actifs fournis.
     * À appeler après toute modification directe d'un lot (correction quantité ou prix).
     */
    Stock recalculateFromLots(Stock stock, List<EntreeStock> activeLots);

    /**
     * Décrémente la quantité disponible du stock agrégé après une sortie.
     * Réservé à la coordination interne au domaine stock.
     */
    Stock decrement(Stock stock, int quantite);
}
