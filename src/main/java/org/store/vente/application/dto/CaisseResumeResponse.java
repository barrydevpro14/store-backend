package org.store.vente.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CaisseResumeResponse(
        UUID magasinId,
        LocalDate from,
        LocalDate to,
        long nombreCommandes,
        long nombreProduits,
        BigDecimal totalCommandes,
        BigDecimal totalPaiements,
        List<PaiementParMoyenResponse> paiementsParMoyen,
        List<VenteParVendeurResponse> ventesParVendeur
) {
}
