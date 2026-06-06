package org.store.vente.application.dto;

import org.store.achat.domain.enums.MoyenPaiement;
import org.store.vente.domain.model.FactureClient;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Paramètres groupés pour la création d'un PaiementVente (regroupe les 4 valeurs au-delà de la règle 30). */
public record PaiementVenteCreate(
        FactureClient facture,
        BigDecimal montant,
        MoyenPaiement moyen,
        LocalDate datePaiement
) {
}
