package org.store.vente.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.store.common.tools.DateHelper;
import org.store.common.validation.DatePattern;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Filtre pour le classement des produits les plus vendus.
 * Supporte deux modes :
 * - journalier : `date` seul (comportement historique)
 * - plage : `startDate` + `endDate` (reporting hebdo / mensuel / personnalisé)
 * En cas de conflit, la plage `startDate`/`endDate` prend le dessus sur `date`.
 */
public record TopProduitsFilter(
        @NotNull UUID magasinId,
        @DatePattern String date,
        @DatePattern String startDate,
        @DatePattern String endDate,
        @Min(1) int nombre
) {
    /** Backward-compat constructor (single-day mode). */
    public TopProduitsFilter(UUID magasinId, String date, int nombre) {
        this(magasinId, date, null, null, nombre);
    }

    /** @deprecated Use {@link #startOfDay()} / {@link #endOfDay()} instead. Kept for test compatibility. */
    @Deprecated(since = "use startOfDay() / endOfDay() instead", forRemoval = false)
    public LocalDate effectiveDate() {
        LocalDateTime parsed = DateHelper.parseStartOfDay(date);
        return parsed != null ? parsed.toLocalDate() : LocalDate.now();
    }

    public LocalDateTime startOfDay() {
        if (startDate != null && !startDate.isBlank()) {
            return DateHelper.parseStartOfDay(startDate);
        }
        LocalDateTime parsed = DateHelper.parseStartOfDay(date);
        return parsed != null ? parsed : LocalDate.now().atStartOfDay();
    }

    public LocalDateTime endOfDay() {
        if (endDate != null && !endDate.isBlank()) {
            return DateHelper.parseEndOfDay(endDate);
        }
        LocalDateTime parsed = DateHelper.parseStartOfDay(date);
        LocalDate effective = parsed != null ? parsed.toLocalDate() : LocalDate.now();
        return effective.atTime(LocalTime.MAX);
    }

    public Pageable toPageable() {
        return PageRequest.of(0, nombre);
    }
}
