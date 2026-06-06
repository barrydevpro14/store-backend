package org.store.achat.application.dto;

import org.store.achat.domain.enums.StatutFacture;
import org.store.achat.domain.model.FactureAchat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record FactureAchatResponse(
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
    public FactureAchatResponse(FactureAchat facture) {
        this(
                facture.getId(),
                facture.getNumero(),
                facture.getStatut(),
                facture.getMontantTotal(),
                facture.getMontantPaye() != null ? facture.getMontantPaye() : BigDecimal.ZERO,
                facture.getMontantTotal().subtract(facture.getMontantPaye() != null ? facture.getMontantPaye() : BigDecimal.ZERO),
                facture.getDate(),
                facture.getDateEcheance(),
                facture.getCommande() != null ? facture.getCommande().getId() : null
        );
    }
}
