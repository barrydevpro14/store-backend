package org.store.stock.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.stock.application.dto.MarginReportFilter;
import org.store.stock.application.dto.MarginReportResponse;
import org.store.stock.application.dto.SortieStockCreate;
import org.store.stock.domain.model.EntreeStock;
import org.store.stock.domain.model.SortieStock;
import org.store.stock.domain.repository.SortieStockRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class SortieStockDomainService extends GlobalService<SortieStock, SortieStockRepository> {
    public SortieStockDomainService(SortieStockRepository repository) {
        super(repository);
    }

    /**
     * Crée une ligne de sortie pour un lot donné avec marge pré-calculée
     * {@code (prixVente - prixAchat) × quantite}, snapshot des prix au moment de la sortie.
     * Le champ ligneVente est nullable (ajustements stock négatifs sans lien vente).
     */
    public SortieStock create(SortieStockCreate sortieStockCreate) {
        EntreeStock lot = sortieStockCreate.lot();
        int quantite = sortieStockCreate.quantite();
        BigDecimal prixVente = sortieStockCreate.prixVente();

        SortieStock sortie = new SortieStock();
        sortie.setEntreeStock(lot);
        sortie.setQuantiteSortie(quantite);
        sortie.setPrixAchat(lot.getPrixAchat());
        sortie.setPrixVente(prixVente);
        sortie.setMarge(prixVente.subtract(lot.getPrixAchat()).multiply(BigDecimal.valueOf(quantite)));
        sortie.setLigneVente(sortieStockCreate.ligneVente());
        return save(sortie);
    }

    /** Surcharge pratique sans lien vente (utilisée pour ajustements stock ou sorties internes). */
    public SortieStock create(EntreeStock lot, int quantite, BigDecimal prixVente) {
        return create(new SortieStockCreate(lot, quantite, prixVente, null));
    }

    public MarginReportResponse computeMargin(MarginReportFilter filter, UUID entrepriseId) {
        return repository.computeMargin(filter, entrepriseId);
    }

    /** Sorties actives (non annulées) liées à une ligne de vente — utilisé pour la ré-injection à l'annulation. */
    public List<SortieStock> findActiveByLigneVenteId(UUID ligneVenteId) {
        return repository.findAllByLigneVenteIdAndAnnuleeFalse(ligneVenteId);
    }

    /** Marque la sortie comme annulée (préserve l'audit, exclut la sortie des reports de marges). */
    public SortieStock markAsAnnulee(SortieStock sortie) {
        sortie.setAnnulee(true);
        return save(sortie);
    }
}
