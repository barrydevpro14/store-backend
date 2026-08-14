package org.store.common.pdf;

import org.store.common.dto.PdfColors;
import org.store.magasin.domain.model.Magasin;
import org.store.pdf.domain.model.PdfFormatConfig;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Contexte passé à AbstractPdfRenderer.addHeader — regroupe toutes les
 * données variables du header (document, magasin, format) pour respecter
 * la règle ≤ 3 paramètres par méthode.
 */
public record PdfHeaderContext(
        Magasin magasin,
        String numeroDoc,
        LocalDate dateDoc,
        LocalTime heureDoc,
        LocalDate dateEcheance,
        String clientLabel,
        String documentLabel,
        PdfColors colors,
        PdfFormatConfig config
) {
}
