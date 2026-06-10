package org.store.paiement.application.dto;

import org.store.paiement.domain.model.MoyenPaiement;

import java.util.UUID;

public record MoyenPaiementResponse(
        UUID id,
        String libelle,
        boolean actif
) {
    public MoyenPaiementResponse(MoyenPaiement moyenPaiement) {
        this(moyenPaiement.getId(), moyenPaiement.getLibelle(), moyenPaiement.isActif());
    }
}
