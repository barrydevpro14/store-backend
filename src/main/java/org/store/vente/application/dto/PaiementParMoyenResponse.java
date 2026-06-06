package org.store.vente.application.dto;

import org.store.achat.domain.enums.MoyenPaiement;

import java.math.BigDecimal;

public record PaiementParMoyenResponse(
        MoyenPaiement moyen,
        BigDecimal total,
        Long nombre
) {
}
