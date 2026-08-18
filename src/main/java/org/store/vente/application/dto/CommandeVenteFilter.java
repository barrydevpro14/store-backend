package org.store.vente.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.store.achat.domain.enums.StatutFacture;
import org.store.common.tools.EnumHelper;
import org.store.common.validation.DatePattern;
import org.store.common.validation.EnumValue;
import org.store.vente.domain.enums.CommandeVenteStatut;

import java.util.UUID;

public record CommandeVenteFilter(
        @NotNull UUID magasinId,
        UUID clientId,
        @EnumValue(enumClass = CommandeVenteStatut.class) String statut,
        @EnumValue(enumClass = StatutFacture.class) String statutFacture,
        String reference,
        @DatePattern String startDate,
        @DatePattern String endDate,
        @Min(0) int page,
        @Min(1) int size
) {
    public CommandeVenteStatut statutAsEnum() {
        return EnumHelper.parse(CommandeVenteStatut.class, statut);
    }

    public StatutFacture statutFactureAsEnum() {
        return EnumHelper.parse(StatutFacture.class, statutFacture);
    }

    public Pageable toPageable() {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
