package org.store.reporting.application.dto;

import jakarta.validation.constraints.NotNull;
import org.store.common.tools.DateHelper;
import org.store.depense.application.dto.DepenseFilter;

import java.time.LocalDate;
import java.util.UUID;

/** Paramètres d'impression d'un rapport : magasin, période choisie et format PDF. */
public record ReportPdfFilter(
        @NotNull UUID magasinId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotNull UUID configId
) {

    public MagasinOverviewFilter toMagasinOverviewFilter() {
        return new MagasinOverviewFilter(magasinId, startDate, endDate);
    }

    public DepenseFilter toDepenseFilter() {
        return new DepenseFilter(magasinId, null, null, null,
                DateHelper.format(startDate), DateHelper.format(endDate), 0, 1);
    }

    public String periodLabel() {
        String start = DateHelper.formatDisplay(startDate);
        return startDate.equals(endDate) ? start : start + " - " + DateHelper.formatDisplay(endDate);
    }
}
