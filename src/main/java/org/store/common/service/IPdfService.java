package org.store.common.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import org.store.magasin.domain.model.Magasin;

import java.awt.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/** Shared PDF building blocks: cells, formatting, footer, and store header cell. */
public interface IPdfService {

    DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    Color PRIMARY   = new Color(37, 99, 235);
    Color LIGHT_BG  = new Color(239, 246, 255);
    Color GRAY_TEXT = new Color(107, 114, 128);
    Color BORDER    = new Color(229, 231, 235);

    String msg(String key);

    String formatAmount(BigDecimal amount);

    boolean isNotBlank(String s);

    String nullToEmpty(String s);

    PdfPCell sectionCell(String title);

    PdfPCell textCell(String text, Font font, Color bg);

    PdfPCell numCell(String text, Font font, Color bg);

    void addTotalRow(PdfPTable table, String label, String value,
                     Font labelFont, Font valueFont, Color bg);

    PdfPCell buildStoreCell(Magasin magasin);

    void addFooter(Document doc, Magasin magasin) throws DocumentException;
}
