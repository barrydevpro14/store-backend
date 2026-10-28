package org.store.reporting.application.dto;

/** Ligne imprimée d'un rapport : libellé à gauche, valeur déjà formatée à droite. */
public record ReportPdfLine(String label, String value) {
}
