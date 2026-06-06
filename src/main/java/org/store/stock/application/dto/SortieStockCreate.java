package org.store.stock.application.dto;

import org.store.stock.domain.model.EntreeStock;
import org.store.vente.domain.model.LigneCommandeVente;

import java.math.BigDecimal;

public record SortieStockCreate(
        EntreeStock lot,
        int quantite,
        BigDecimal prixVente,
        LigneCommandeVente ligneVente
) {
}
