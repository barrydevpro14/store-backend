package org.store.reporting.application.dto;

import java.util.List;

/** Contenu d'un rapport à imprimer, indépendant du format (A4, A5, thermique). */
public record ReportPdfDocument(
        String title,
        String periodLabel,
        List<ReportPdfSection> sections
) {
}
