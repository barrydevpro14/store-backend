package org.store.inventaire.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.store.common.tools.EnumHelper;
import org.store.common.validation.DatePattern;
import org.store.common.validation.EnumValue;
import org.store.inventaire.domain.enums.InventaireStatut;

import java.util.UUID;

public record InventaireFilter(
        @NotNull UUID magasinId,
        @EnumValue(enumClass = InventaireStatut.class) String statut,
        @DatePattern String startDate,
        @DatePattern String endDate,
        @Min(0) int page,
        @Min(1) int size
) {
    public InventaireStatut statutAsEnum() {
        return EnumHelper.parse(InventaireStatut.class, statut);
    }

    public Pageable toPageable() {
        return PageRequest.of(page, size);
    }
}
