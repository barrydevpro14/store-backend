package org.store.inventaire.application.dto;

import jakarta.validation.constraints.Size;

public record CloturerRequest(
        @Size(max = 1000) String commentaire
) {
}
