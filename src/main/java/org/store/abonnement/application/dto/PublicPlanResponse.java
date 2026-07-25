package org.store.abonnement.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PublicPlanResponse(
        UUID id,
        String nom,
        String description,
        BigDecimal prix,
        int nombreMagasinsMax,
        int nombreEmployesMax,
        boolean gestionStock,
        boolean gestionVente,
        boolean gestionAchat,
        boolean gestionComptabilite,
        int ordre
) {}
