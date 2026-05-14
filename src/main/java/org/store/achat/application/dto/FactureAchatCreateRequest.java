package org.store.achat.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record FactureAchatCreateRequest(
        @NotBlank @Size(max = 100) String numero,
        @NotNull LocalDate date,
        LocalDate dateEcheance
) {
}
