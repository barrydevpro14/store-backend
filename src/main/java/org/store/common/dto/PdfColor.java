package org.store.common.dto;

import java.awt.Color;

/**
 * Palette de couleurs partagée par tous les générateurs PDF.
 * Chaque constante encapsule un {@link Color} AWT réutilisable via {@link #color()}.
 */
public enum PdfColor {

    PRIMARY(37, 99, 235),
    LIGHT_BG(239, 246, 255),
    GRAY_TEXT(107, 114, 128),
    BORDER(229, 231, 235);

    private final Color color;

    PdfColor(int r, int g, int b) {
        this.color = new Color(r, g, b);
    }

    public Color color() {
        return color;
    }
}
