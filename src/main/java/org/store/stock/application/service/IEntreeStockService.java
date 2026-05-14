package org.store.stock.application.service;

import org.store.stock.application.dto.EntreeStockRequest;
import org.store.stock.application.dto.EntreeStockResponse;

public interface IEntreeStockService {

    /**
     * Création d'une entrée stock manuelle : crée le lot (EntreeStock), met à jour le Stock agrégé
     * (quantité disponible + prix d'achat moyen pondéré) et journalise le mouvement (ENTREE_ACHAT).
     */
    EntreeStockResponse create(EntreeStockRequest entreeStockRequest);
}
