package org.store.stock.application.dto;

import org.store.achat.application.dto.FournisseurSummaryResponse;
import org.store.common.tools.DateHelper;
import org.store.stock.domain.model.SortieStock;

import java.math.BigDecimal;
import java.util.UUID;

public record SortieStockResponse(
        UUID id,
        UUID entreeStockId,
        String numeroLot,
        FournisseurSummaryResponse fournisseur,
        int quantiteSortie,
        BigDecimal prixAchat,
        BigDecimal prixVente,
        BigDecimal marge,
        String createdAt
) {
    public SortieStockResponse(SortieStock sortie) {
        this(
                sortie.getId(),
                sortie.getEntreeStock().getId(),
                sortie.getEntreeStock().getNumeroLot(),
                sortie.getEntreeStock().getProductFournisseur() != null
                        ? new FournisseurSummaryResponse(sortie.getEntreeStock().getProductFournisseur().getFournisseur())
                        : null,
                sortie.getQuantiteSortie(),
                sortie.getPrixAchat(),
                sortie.getPrixVente(),
                sortie.getMarge(),
                DateHelper.format(sortie.getCreatedAt())
        );
    }
}
