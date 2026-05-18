package org.store.achat.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.store.achat.domain.enums.MotifAnnulationAchat;
import org.store.common.validation.EnumValue;

public record AnnulationAchatRequest(
        @NotBlank
        @EnumValue(enumClass = MotifAnnulationAchat.class)
        String motif,

        @Size(max = 1000)
        String commentaire
) {
    public MotifAnnulationAchat motifAsEnum() {
        return MotifAnnulationAchat.valueOf(motif);
    }
}
