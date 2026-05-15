package org.store.stock.application.service;

import org.store.stock.application.dto.EntreeStockRequest;
import org.store.stock.application.dto.EntreeStockResponse;
import org.store.stock.domain.model.EntreeStock;

import java.util.List;
import java.util.UUID;

public interface IEntreeStockService {

    /**
     * Création d'une entrée stock manuelle : crée le lot (EntreeStock), met à jour le Stock agrégé
     * (quantité disponible + prix d'achat moyen pondéré) et journalise le mouvement (ENTREE_ACHAT).
     */
    EntreeStockResponse create(EntreeStockRequest entreeStockRequest);

    /**
     * Retourne les lots actifs (quantiteRestante > 0) d'un magasin pour la liste de produits donnée,
     * fetch joints sur productFournisseur/fournisseur/quality. Utilisé par la recherche produit vendeur.
     */
    List<EntreeStock> findActiveLotsByMagasinAndProductIds(UUID magasinId, List<UUID> productIds);
}
