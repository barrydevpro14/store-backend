package org.store.abonnement.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import org.store.achat.domain.enums.MoyenPaiement;
import org.store.common.validation.EnumValue;

import java.time.LocalDate;

public record PaiementAbonnementRequest(
        @NotNull @EnumValue(enumClass = MoyenPaiement.class) String moyen,
        @Size(max = 255) String referenceTransaction,
        @NotNull @PastOrPresent LocalDate datePaiement
) {
    public MoyenPaiement moyenAsEnum() {
        return MoyenPaiement.valueOf(moyen);
    }
}
