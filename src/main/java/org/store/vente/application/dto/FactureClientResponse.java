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
        this(
                facture.getId(),
                facture.getNumero(),
                facture.getStatut(),
                facture.getMontantTotal(),
                facture.getMontantPaye() != null ? facture.getMontantPaye() : BigDecimal.ZERO,
                facture.getMontantTotal().subtract(facture.getMontantPaye() != null ? facture.getMontantPaye() : BigDecimal.ZERO),
                facture.getDate(),
                facture.getDateEcheache(),
                facture.getCommande() != null ? facture.getCommande().getId() : null
        );
    }
}
