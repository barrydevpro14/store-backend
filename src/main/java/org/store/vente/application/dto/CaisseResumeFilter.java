package org.store.vente.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.store.common.tools.DateHelper;
import org.store.common.validation.DatePattern;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Filtre unique pour les agrégations de caisse (journalier comme multi-jours).
 * `from` obligatoire. `to` optionnel : si absent, on agrège uniquement sur la
 * journée `from` (équivalent journalier). Les queries utilisent
 * createdAt >= startOfPeriod AND createdAt <= endOfPeriod (bornes inclusives jour entier).
 */
public record CaisseResumeFilter(
        @NotNull UUID magasinId,
        @NotBlank @DatePattern String from,
        @DatePattern String to
) {

    public LocalDate fromAsLocalDate() {
        return DateHelper.parseStartOfDay(from).toLocalDate();
    }

    /** Date de fin effective : `to` si renseigné, sinon `from` (résumé sur une seule journée). */
    public LocalDate toAsLocalDate() {
        return effectiveTo() != null
                ? DateHelper.parseStartOfDay(effectiveTo()).toLocalDate()
                : fromAsLocalDate();
    }

    public LocalDateTime startOfPeriod() {
        return DateHelper.parseStartOfDay(from);
    }

    /** Borne fin de période : endOfDay(to) si renseigné, sinon endOfDay(from). */
    public LocalDateTime endOfPeriod() {
        return DateHelper.parseEndOfDay(effectiveTo() != null ? effectiveTo() : from);
    }

    private String effectiveTo() {
        return to != null && !to.isBlank() ? to : null;
    }
}
