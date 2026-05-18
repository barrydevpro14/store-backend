package org.store.vente.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.store.common.validation.EnumValue;
import org.store.vente.domain.enums.MotifAnnulationVente;

public record AnnulationVenteRequest(
        @NotBlank
        @EnumValue(enumClass = MotifAnnulationVente.class)
        String motif,

        @Size(max = 1000)
        String commentaire
) {
    public MotifAnnulationVente motifAsEnum() {
        return MotifAnnulationVente.valueOf(motif);
    }
}
