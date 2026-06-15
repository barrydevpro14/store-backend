package org.store.achat.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.store.achat.domain.enums.CommandeAchatStatut;
import org.store.achat.domain.enums.StatutFacture;
import org.store.common.tools.DateHelper;
import org.store.common.tools.EnumHelper;
import org.store.common.validation.DatePattern;
import org.store.common.validation.EnumValue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record CommandeAchatFilter(
        @NotNull UUID magasinId,
        UUID fournisseurId,
        @EnumValue(enumClass = CommandeAchatStatut.class) String statut,
        @EnumValue(enumClass = StatutFacture.class) String statutFacture,
        String reference,
        @DatePattern String startDate,
        @DatePattern String endDate,
        @Min(0) int page,
        @Min(1) int size
) {
    public CommandeAchatStatut statutAsEnum() {
        return EnumHelper.parse(CommandeAchatStatut.class, statut);
    }

    public StatutFacture statutFactureAsEnum() {
        return EnumHelper.parse(StatutFacture.class, statutFacture);
    }


    public Pageable toPageable() {
        return PageRequest.of(page, size);
    }
}
