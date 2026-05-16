package org.store.vente.application.dto;

import org.store.achat.domain.enums.StatutFacture;
import org.store.vente.domain.model.FactureClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record FactureClientResponse(
        UUID id,
        String numero,
        StatutFacture statut,
        BigDecimal montantTotal,
        BigDecimal montantPaye,
        BigDecimal montantRestant,
        LocalDate date,
        LocalDate dateEcheance,
        UUID commandeId
) {
    public FactureClientResponse(FactureClient facture) {
        this(facture,
                facture.getMontantPaye() != null ? facture.getMontantPaye() : BigDecimal.ZERO);
    }

    private FactureClientResponse(FactureClient facture, BigDecimal montantPaye) {
        this(
                facture.getId(),
                facture.getNumero(),
                facture.getStatut(),
                facture.getMontantTotal(),
                montantPaye,
                facture.getMontantTotal().subtract(montantPaye),
                facture.getDate(),
                facture.getDateEcheance(),
                facture.getCommande() != null ? facture.getCommande().getId() : null
        );
    }
}
