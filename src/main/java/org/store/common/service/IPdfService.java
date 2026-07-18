package org.store.common.service;

import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.store.common.dto.PdfColors;
import org.store.magasin.domain.model.Magasin;

import java.awt.*;
import java.math.BigDecimal;

/** Shared PDF building blocks: cells, formatting, footer, and store header cell. */
public interface IPdfService {

    String msg(String key);

    String formatAmount(BigDecimal amount);

    boolean isNotBlank(String s);

    String nullToEmpty(String s);

    PdfPCell sectionCell(String title);

    PdfPCell textCell(String text, Font font, Color bg);

    PdfPCell numCell(String text, Font font, Color bg);

    void addTotalRow(PdfPTable table, String label, String value,
                     Font labelFont, Font valueFont, Color bg);

    /**
     * Builds the store/enterprise header cell using the provided colour set.
     * Shows logo, sigle, raison sociale, NINEA, RCCM, and address.
     */
    PdfPCell buildStoreCell(Magasin magasin, PdfColors colors);

    /**
     * Resolves a {@link PdfColors} from a hex string (e.g. {@code #2563EB}).
     * Returns {@link PdfColors#defaults()} if {@code couleurPrimaire} is null, blank, or invalid.
     */
    PdfColors resolveColors(String couleurPrimaire);

    /** Registers a PdfPageEventHelper on the writer that draws the footer on every page. Must be called before doc.open(). */
    void configureFooter(PdfWriter writer, Magasin magasin);
}
