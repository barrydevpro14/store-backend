package org.store.stock.application.service;

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
}
