package org.store.reporting.application.dto;

import java.util.List;

/** Bloc d'un rapport imprimé (KPI, répartition…) : titre optionnel, en-têtes de colonnes et lignes. */
public record ReportPdfSection(
        String title,
        String labelHeader,
        String valueHeader,
        List<ReportPdfLine> lines
) {
}
