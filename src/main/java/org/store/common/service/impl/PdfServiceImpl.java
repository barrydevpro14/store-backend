package org.store.common.service.impl;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import org.springframework.stereotype.Service;
import org.store.common.i18n.IMessageSourceService;
import org.store.common.service.IPdfService;
import org.store.magasin.domain.model.Magasin;

import java.awt.*;
import java.math.BigDecimal;

/**
 * Shared PDF building blocks (cells, formatting, footer, store header cell) used by all PDF generators.
 */
@Service
public class PdfServiceImpl implements IPdfService {

    private final IMessageSourceService messageSourceService;

    public PdfServiceImpl(IMessageSourceService messageSourceService) {
        this.messageSourceService = messageSourceService;
    }

    @Override
    public String msg(String key) {
        return messageSourceService.getMessage(key);
    }

    @Override
    public String formatAmount(BigDecimal amount) {
        if (amount == null) return "0";
        return String.format("%,.0f", amount);
    }

    @Override
    public boolean isNotBlank(String s) { return s != null && !s.isBlank(); }

    @Override
    public String nullToEmpty(String s) { return s == null ? "" : s; }

    @Override
    public PdfPCell sectionCell(String title) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(BORDER);
        cell.setPadding(10);
        cell.addElement(new Paragraph(title, new Font(Font.HELVETICA, 8, Font.BOLD, GRAY_TEXT)));
        cell.addElement(Chunk.NEWLINE);
        return cell;
    }

    @Override
    public PdfPCell textCell(String text, Font font, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorderColor(BORDER);
        cell.setBackgroundColor(bg);
        cell.setPadding(6);
        return cell;
    }

    @Override
    public PdfPCell numCell(String text, Font font, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setBorderColor(BORDER);
        cell.setBackgroundColor(bg);
        cell.setPadding(6);
        return cell;
    }

    @Override
    public void addTotalRow(PdfPTable table, String label, String value,
                            Font labelFont, Font valueFont, Color bg) {
        PdfPCell lc = new PdfPCell(new Phrase(label, labelFont));
        lc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        lc.setBorderColor(BORDER);
        lc.setBackgroundColor(bg);
        lc.setPadding(6);
        table.addCell(lc);

        PdfPCell vc = new PdfPCell(new Phrase(value, valueFont));
        vc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        vc.setBorderColor(BORDER);
        vc.setBackgroundColor(bg);
        vc.setPadding(6);
        table.addCell(vc);
    }

    @Override
    public PdfPCell buildStoreCell(Magasin magasin) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(8);
        cell.addElement(new Paragraph(magasin.getNom(), new Font(Font.HELVETICA, 16, Font.BOLD, PRIMARY)));
        Font infoFont = new Font(Font.HELVETICA, 9, Font.NORMAL, GRAY_TEXT);
        if (isNotBlank(magasin.getAdresse()))
            cell.addElement(new Paragraph(magasin.getAdresse(), infoFont));
        if (isNotBlank(magasin.getTelephone()))
            cell.addElement(new Paragraph(magasin.getTelephone(), infoFont));
        return cell;
    }

    @Override
    public void addFooter(Document doc, Magasin magasin) throws DocumentException {
        doc.add(Chunk.NEWLINE);
        Font footerFont = new Font(Font.HELVETICA, 8, Font.ITALIC, GRAY_TEXT);
        String text = msg("pdf.label.footer");
        if (isNotBlank(magasin.getNom())) text = magasin.getNom() + " – " + text;
        Paragraph footer = new Paragraph(text, footerFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer);
    }
}
