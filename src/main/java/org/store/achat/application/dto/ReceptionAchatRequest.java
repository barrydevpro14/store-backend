package org.store.achat.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ReceptionAchatRequest(
        @NotEmpty @Valid List<LigneReceptionRequest> lignes
) {
}
