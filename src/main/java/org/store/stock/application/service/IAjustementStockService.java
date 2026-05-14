package org.store.stock.application.service;

import org.store.stock.application.dto.AjustementStockRequest;
import org.store.stock.application.dto.MouvementStockResponse;

public interface IAjustementStockService {

    /**
     * Ajuste manuellement le stock d'un produit dans un magasin pour aligner stock théorique
     * et stock physique. POSITIF : crée une entrée lot (fournisseur requis), met à jour le stock.
     * NEGATIF : consomme les lots FIFO sans créer de SortieStock. Toujours journalisé en
     * MouvementStock(AJUSTEMENT) avec le motif.
     */
    MouvementStockResponse create(AjustementStockRequest ajustementStockRequest);
}
