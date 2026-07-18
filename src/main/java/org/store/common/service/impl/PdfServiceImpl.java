package org.store.common.service.impl;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import org.store.common.dto.PdfColor;
import org.store.common.dto.PdfColors;
import org.store.common.i18n.IMessageSourceService;
import org.store.common.service.IPdfService;
import org.store.entreprise.domain.model.Entreprise;
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
        cell.setBorderColor(PdfColor.BORDER.color());
        cell.setPadding(10);
        cell.addElement(new Paragraph(title, new Font(Font.HELVETICA, 8, Font.BOLD, PdfColor.GRAY_TEXT.color())));
        cell.addElement(Chunk.NEWLINE);
        return cell;
    }

    @Override
    public PdfPCell textCell(String text, Font font, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorderColor(PdfColor.BORDER.color());
        cell.setBackgroundColor(bg);
        cell.setPadding(6);
        return cell;
    }

    @Override
    public PdfPCell numCell(String text, Font font, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setBorderColor(PdfColor.BORDER.color());
        cell.setBackgroundColor(bg);
        cell.setPadding(6);
        return cell;
    }

    @Override
    public void addTotalRow(PdfPTable table, String label, String value,
                            Font labelFont, Font valueFont, Color bg) {
        PdfPCell lc = new PdfPCell(new Phrase(label, labelFont));
        lc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        lc.setBorderColor(PdfColor.BORDER.color());
        lc.setBackgroundColor(bg);
        lc.setPadding(6);
        table.addCell(lc);

        PdfPCell vc = new PdfPCell(new Phrase(value, valueFont));
        vc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        vc.setBorderColor(PdfColor.BORDER.color());
        vc.setBackgroundColor(bg);
        vc.setPadding(6);
        table.addCell(vc);
    }

    @Override
    public PdfPCell buildStoreCell(Magasin magasin, PdfColors colors) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBackgroundColor(colors.lightBg());
        cell.setPadding(12);

        Entreprise entreprise = magasin.getEntreprise();

        addLogoIfPresent(cell, entreprise);

        if (isNotBlank(entreprise.getSigle()))
            cell.addElement(new Paragraph(entreprise.getSigle(), new Font(Font.HELVETICA, 16, Font.BOLD, colors.primary())));

        Font infoFont = new Font(Font.HELVETICA, 9, Font.NORMAL, colors.primary());

        if (isNotBlank(entreprise.getRaisonSociale()))
            cell.addElement(new Paragraph(entreprise.getRaisonSociale(), new Font(Font.HELVETICA, 10, Font.NORMAL, colors.primary())));
        if (isNotBlank(entreprise.getNinea()))
            cell.addElement(new Paragraph(msg("pdf.label.ninea") + " : " + entreprise.getNinea(), infoFont));
        if (isNotBlank(entreprise.getRccm()))
            cell.addElement(new Paragraph(msg("pdf.label.rccm") + " : " + entreprise.getRccm(), infoFont));
        if (isNotBlank(entreprise.getAdresse()))
            cell.addElement(new Paragraph(msg("pdf.label.adresse") + " : " + entreprise.getAdresse(), infoFont));

        return cell;
    }

    @Override
    public PdfColors resolveColors(String couleurPrimaire) {
        if (couleurPrimaire == null || couleurPrimaire.isBlank()) return PdfColors.defaults();
        try {
            Color primary = hexToColor(couleurPrimaire);
            return new PdfColors(primary, lighten(primary, 0.85f));
        } catch (NumberFormatException ignored) {
            return PdfColors.defaults();
        }
    }

    private static Color hexToColor(String hex) {
        int r = Integer.parseInt(hex.substring(1, 3), 16);
        int g = Integer.parseInt(hex.substring(3, 5), 16);
        int b = Integer.parseInt(hex.substring(5, 7), 16);
        return new Color(r, g, b);
    }

    /** Blends {@code color} toward white by {@code factor} (0 = original, 1 = white). */
    private static Color lighten(Color color, float factor) {
        int r = Math.round(color.getRed()   + (255 - color.getRed())   * factor);
        int g = Math.round(color.getGreen() + (255 - color.getGreen()) * factor);
        int b = Math.round(color.getBlue()  + (255 - color.getBlue())  * factor);
        return new Color(r, g, b);
    }

    /** Renders the entreprise logo above the company name; silently skips if absent or unreadable. */
    private void addLogoIfPresent(PdfPCell cell, Entreprise entreprise) {
        if (entreprise.getLogo() == null || entreprise.getLogo().getDocument() == null) return;
        try {
            Image logo = Image.getInstance(entreprise.getLogo().getDocument());
            logo.scaleToFit(100, 60);
            cell.addElement(logo);
        } catch (Exception ignored) { }
    }

    @Override
    public void configureFooter(PdfWriter writer, Magasin magasin) {
        writer.setPageEvent(new FooterPageEvent(buildFooterText(magasin), msg("pdf.label.cachetSignature")));
    }

    private String buildFooterText(Magasin magasin) {
        StringBuilder sb = new StringBuilder(msg("pdf.label.footer"));
        if (isNotBlank(magasin.getNom())) sb.append(" ").append(magasin.getNom());
        if (isNotBlank(magasin.getTelephone())) sb.append(" – ").append(magasin.getTelephone());
        if (isNotBlank(magasin.getAdresse())) sb.append(" – ").append(magasin.getAdresse());
        return sb.toString();
    }

    private static final class FooterPageEvent extends PdfPageEventHelper {

        private final String footerText;
        private final String stampLabel;
        private static final Font FOOTER_FONT  = new Font(Font.HELVETICA, 9, Font.ITALIC,  PdfColor.GRAY_TEXT.color());
        private static final Font LABEL_FONT   = new Font(Font.HELVETICA, 8, Font.NORMAL,  PdfColor.GRAY_TEXT.color());
        private static final float BOX_HEIGHT  = 55f;
        private static final float BOX_GAP     = 20f;
        private static final float FOOTER_OFFSET = 12f;

        FooterPageEvent(String footerText, String stampLabel) {
            this.footerText = footerText;
            this.stampLabel = stampLabel;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            float left     = document.left();
            float right    = document.right();
            float boxWidth = (right - left - BOX_GAP) / 2f;
            float boxTop   = document.bottom() - 8f;
            float boxBottom = boxTop - BOX_HEIGHT;

            drawDashedBox(cb, left, boxBottom, boxWidth, BOX_HEIGHT);
            drawDashedBox(cb, left + boxWidth + BOX_GAP, boxBottom, boxWidth, BOX_HEIGHT);

            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase(stampLabel, LABEL_FONT), left + 4, boxTop - 9, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase(stampLabel, LABEL_FONT), left + boxWidth + BOX_GAP + 4, boxTop - 9, 0);

            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    new Phrase(footerText, FOOTER_FONT),
                    (left + right) / 2f, boxBottom - FOOTER_OFFSET, 0);
        }

        private static void drawDashedBox(PdfContentByte cb, float x, float y, float w, float h) {
            cb.saveState();
            cb.setLineDash(3f, 3f);
            cb.setLineWidth(0.5f);
            cb.setColorStroke(PdfColor.GRAY_TEXT.color());
            cb.rectangle(x, y, w, h);
            cb.stroke();
            cb.restoreState();
        }
    }
}
