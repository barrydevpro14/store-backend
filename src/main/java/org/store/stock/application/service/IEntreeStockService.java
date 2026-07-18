package org.store.stock.application.service;

import org.store.stock.application.dto.EntreeStockCreate;
import org.store.stock.application.dto.EntreeStockRequest;
import org.store.stock.application.dto.EntreeStockResponse;
import org.store.stock.domain.model.EntreeStock;

import java.util.List;
import java.util.UUID;

public interface IEntreeStockService {

    /**
     * Enregistrement d'entrées stock multi-lignes pour un même fournisseur, sans passer par une CommandeAchat
     * (cas du démarrage d'un magasin avec stock physique préexistant). Pour chaque ligne : findOrCreate du
     * ProductFournisseur, création du lot (EntreeStock), upsert du Stock agrégé (PMP) et journal ENTREE_INITIAL.
     */
    List<EntreeStockResponse> create(EntreeStockRequest entreeStockRequest);

    /**
     * Retourne les lots actifs (quantiteRestante > 0) d'un magasin pour la liste de produits donnée,
     * fetch joints sur productFournisseur/fournisseur/quality. Utilisé par la recherche produit vendeur.
     */
    List<EntreeStock> findActiveLotsByMagasinAndProductIds(UUID magasinId, List<UUID> productIds);

    /**
     * Retourne les lots disponibles (quantiteRestante > 0) triés FIFO pour un (magasin, productFournisseur).
     * Réservé à la coordination interne au domaine stock.
     */
    List<EntreeStock> findAvailableLotsForFifo(UUID magasinId, UUID productFournisseurId);

    /**
     * Persiste un lot dont la quantiteRestante a été modifiée (consommation FIFO).
     * Réservé à la coordination interne au domaine stock.
     */
    void saveLot(EntreeStock lot);

    /**
     * Crée un lot EntreeStock à partir d'un contexte groupé (achat, entrée initiale).
     * Réservé à la coordination interne au domaine stock.
     */
    EntreeStock createEntreeStock(EntreeStockCreate entreeStockCreate);

    /**
     * Retourne tous les lots issus d'une commande d'achat donnée (utilisé par l'annulation d'achat).
     * Réservé à la coordination interne au domaine stock.
     */
    List<EntreeStock> findByCommandeAchatId(UUID commandeAchatId);

    /**
     * Marque un lot comme annulé (lot retiré du stock + retourné au fournisseur).
     * Réservé à la coordination interne au domaine stock.
     */
    void markAsAnnulee(EntreeStock lot);
}
