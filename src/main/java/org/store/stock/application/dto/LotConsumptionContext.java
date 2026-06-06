package org.store.stock.application.dto;

import org.store.vente.domain.model.LigneCommandeVente;

import java.math.BigDecimal;

/** Paramètres communs d'une consommation FIFO : quantite cible, prix snapshot, ligne de vente optionnelle. */
public record LotConsumptionContext(
        int totalAConsommer,
        BigDecimal prixVente,
        LigneCommandeVente ligneVente
) {
}
