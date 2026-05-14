package org.store.stock.application.service;

import org.store.stock.application.dto.SortieStockRequest;
import org.store.stock.application.dto.SortieStockResponse;

import java.util.List;

public interface ISortieStockService {

    /**
     * Sortie de stock FIFO : consomme les EntreeStock du plus ancien au plus récent jusqu'à
     * la quantité demandée. Décrémente le stock agrégé et journalise un mouvement SORTIE_VENTE.
     * Retourne la liste des sorties (une par lot consommé).
     */
    List<SortieStockResponse> create(SortieStockRequest sortieStockRequest);
}
