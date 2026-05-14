package org.store.stock.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.stock.application.dto.MarginReportFilter;
import org.store.stock.application.dto.MarginReportResponse;
import org.store.stock.domain.model.EntreeStock;
import org.store.stock.domain.model.SortieStock;
import org.store.stock.domain.repository.SortieStockRepository;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class SortieStockDomainService extends GlobalService<SortieStock, SortieStockRepository> {
    public SortieStockDomainService(SortieStockRepository repository) {
        super(repository);
    }

    /**
     * Crée une ligne de sortie pour un lot donné avec marge pré-calculée
     * {@code (prixVente - prixAchat) × quantite}, snapshot des prix au moment de la sortie.
     */
    public SortieStock create(EntreeStock lot, int quantite, BigDecimal prixVente) {
        SortieStock sortie = new SortieStock();
        sortie.setEntreeStock(lot);
        sortie.setQuantiteSortie(quantite);
        sortie.setPrixAchat(lot.getPrixAchat());
        sortie.setPrixVente(prixVente);
        sortie.setMarge(prixVente.subtract(lot.getPrixAchat()).multiply(BigDecimal.valueOf(quantite)));
        return save(sortie);
    }

    public MarginReportResponse computeMargin(MarginReportFilter filter, UUID entrepriseId) {
        return repository.computeMargin(filter, entrepriseId);
    }
}
