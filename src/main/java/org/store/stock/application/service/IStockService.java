package org.store.stock.application.service;

import org.springframework.data.domain.Page;
import org.store.stock.application.dto.StockFilter;
import org.store.stock.application.dto.StockResponse;

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
}
