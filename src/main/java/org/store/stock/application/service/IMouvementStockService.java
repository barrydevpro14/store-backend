package org.store.stock.application.service;

import org.springframework.data.domain.Page;
import org.store.stock.application.dto.MouvementJournalize;
import org.store.stock.application.dto.MouvementStockFilter;
import org.store.stock.application.dto.MouvementStockResponse;
import org.store.stock.domain.model.Stock;

public interface IMouvementStockService {

    /**
     * Liste paginée du journal des mouvements de stock à partir d'un filter validé.
     * Le filter porte les critères (magasin, produit, stock, type, période) + la pagination (page, size).
     * Si le caller est un employé sans filtre magasin/stock explicite, le scope est forcé sur son magasin.
     */
    Page<MouvementStockResponse> findAllByCurrentEntreprise(MouvementStockFilter filter);

    /**
     * Journalise un mouvement de stock et retourne la réponse DTO.
     * Réservé à la coordination interne au domaine stock.
     */
    MouvementStockResponse journalize(Stock stock, MouvementJournalize mouvementJournalize);
}
