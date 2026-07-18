package org.store.common.dto;

import org.store.common.dto.PdfColor;

import java.awt.*;

/** Resolved colour set for a given enterprise. Use {@link PdfColors#defaults()} as fallback. */
public record PdfColors(Color primary, Color lightBg) {

    public static PdfColors defaults() {
        return new PdfColors(PdfColor.PRIMARY.color(), PdfColor.LIGHT_BG.color());
    }
}
