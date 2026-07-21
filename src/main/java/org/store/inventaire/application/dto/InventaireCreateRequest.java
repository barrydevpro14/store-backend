package org.store.inventaire.application.dto;

import jakarta.validation.constraints.NotNull;
import org.store.inventaire.domain.enums.TypeInventaire;

import java.util.UUID;

public record InventaireCreateRequest(
        @NotNull UUID magasinId,
        @NotNull TypeInventaire type
) {}
