package org.store.vente.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import org.store.achat.domain.enums.MoyenPaiement;
import org.store.common.validation.EnumValue;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaiementVenteRequest(
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal montant,
        @NotBlank @EnumValue(enumClass = MoyenPaiement.class) String modePaiement,
        @PastOrPresent LocalDate datePaiement
) {
    public MoyenPaiement modePaiementAsEnum() {
        return MoyenPaiement.valueOf(modePaiement);
    }
}
