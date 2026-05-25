package org.store.achat.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Command record for updating a ligne d'achat: quantité, prix, traçabilité lot. */
public record LigneCommandeAchatUpdate(
        int quantite,
        BigDecimal prixAchat,
        BigDecimal prixVente,
        String numeroLot,
        LocalDate dateExpiration
) {
}
