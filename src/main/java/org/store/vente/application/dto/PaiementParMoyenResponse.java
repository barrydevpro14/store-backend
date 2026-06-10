package org.store.vente.application.dto;

import org.store.paiement.domain.model.MoyenPaiement;

import java.math.BigDecimal;

public record PaiementParMoyenResponse(
        MoyenPaiement moyen,
        BigDecimal total,
        Long nombre
) {
}
