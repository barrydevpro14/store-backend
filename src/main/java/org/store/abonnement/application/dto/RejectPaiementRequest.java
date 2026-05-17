package org.store.abonnement.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectPaiementRequest(
        @NotBlank @Size(max = 1000) String motifRejet
) {
}
