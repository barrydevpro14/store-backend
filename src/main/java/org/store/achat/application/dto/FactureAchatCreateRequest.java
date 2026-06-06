package org.store.achat.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Payload de création d'une `FactureAchat` au moment du `validate` d'une commande.
 * `numero` est optionnel : laissé vide, le backend génère automatiquement un numéro
 * unique au format `FACT-yyyyMMdd-HHmmssSSS` (cf. {@code ReferenceHelper}).
 */
public record FactureAchatCreateRequest(
        @Size(max = 100) String numero,
        @NotNull LocalDate date,
        @NotNull LocalDate dateEcheance
) {
}
