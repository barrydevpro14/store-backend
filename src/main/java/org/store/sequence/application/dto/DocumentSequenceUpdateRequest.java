package org.store.sequence.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record DocumentSequenceUpdateRequest(
        @NotBlank String prefixe,
        @Min(1) long prochaineSequence,
        @Min(1) int longueurSequence
) {
}
