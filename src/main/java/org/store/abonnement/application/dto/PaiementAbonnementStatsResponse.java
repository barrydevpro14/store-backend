package org.store.abonnement.application.dto;

import java.math.BigDecimal;

public record PaiementAbonnementStatsResponse(Long valides, BigDecimal revenu) {
}
