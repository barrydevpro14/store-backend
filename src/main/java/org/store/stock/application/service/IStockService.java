package org.store.stock.application.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.store.stock.application.dto.StockResponse;

import java.util.UUID;

public interface IStockService {

    /**
     * Lecture d'un stock par id, scopée sur l'entreprise du caller (et sur son magasin propre s'il est employé).
     */
    StockResponse findResponseById(UUID id);

    /**
     * Liste paginée des stocks de l'entreprise du caller, filtrable par magasin et/ou produit.
     * Si le caller est un employé sans filtre magasin explicite, le filtre est forcé sur son magasin.
     */
    Page<StockResponse> findAllByCurrentEntreprise(UUID magasinId, UUID productId, Pageable pageable);
}
