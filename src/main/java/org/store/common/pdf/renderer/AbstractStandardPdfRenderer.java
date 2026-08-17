package org.store.common.pdf.renderer;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.store.common.dto.PdfColor;
import org.store.common.pdf.PdfHeaderContext;
import org.store.common.service.IPdfService;
import org.store.common.tools.DateHelper;
import org.store.entreprise.domain.model.Entreprise;
import org.store.magasin.domain.model.Magasin;
import org.store.pdf.domain.model.PdfFormatConfig;

import java.awt.*;
import java.io.ByteArrayOutputStream;

/**
 * Classe de base pour les renderers A4 et A5.
 * Hauteur de page fixe, footer, header avec centrage 3 colonnes si logo présent.
 */
public abstract class AbstractStandardPdfRenderer extends AbstractPdfRenderer {

    protected AbstractStandardPdfRenderer(IPdfService pdf) {
        super(pdf);
    }

    protected byte[] buildDocument(PdfFormatConfig config, Magasin magasin, DocumentConsumer consumer) {
        float w = config.getPageWidth().floatValue();
        float h = config.getPageHeight().floatValue();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(
                    new Rectangle(w, h),
                    config.getMarginLeft().floatValue(),
                    config.getMarginRight().floatValue(),
                    config.getMarginTop().floatValue(),
                    config.getMarginBottom().floatValue()
            );
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            pdf.configureFooter(writer, magasin);
            doc.open();
            consumer.accept(doc);
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    /**
     * Header complet A4/A5 :
     *   1. Logo (gauche 20%) + infos entreprise (centre 60%) + miroir vide (droite 20%)
     *   2. Label du document
     *   3. Tableau méta : NUMÉRO | DATE/HEURE | ÉCHÉANCE | MAGASIN
     *   4. Ligne CLIENT (si non vide)
     */
    protected void addHeader(Document doc, PdfHeaderContext ctx) throws DocumentException {
        doc.add(buildStoreRow(ctx));
        doc.add(buildDocumentLabelRow(ctx));
        doc.add(buildMetaTable(ctx));
        if (pdf.isNotBlank(ctx.clientLabel())) {
            doc.add(buildClientRow(ctx));
        }
    }

    /* ── Store row ─────────────────────────────────────────────────────── */

    private PdfPTable buildStoreRow(PdfHeaderContext ctx) throws DocumentException {
        Entreprise e = ctx.magasin().getEntreprise();
        boolean hasLogo = e.getLogo() != null && e.getLogo().getDocument() != null;

        if (hasLogo) {
            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{20, 60, 20});
            table.addCell(buildLogoCell(ctx));
            table.addCell(buildEnterpriseInfoCell(ctx));
            table.addCell(emptyCell());
            return table;
        }

        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.addCell(buildEnterpriseInfoCell(ctx));
        return table;
    }

    private PdfPCell emptyCell() {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }

    private PdfPCell buildLogoCell(PdfHeaderContext ctx) {
        Entreprise e = ctx.magasin().getEntreprise();
        float normalSize = ctx.config().getFontSizeNormal().floatValue();

        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(8);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        if (e.getLogo() != null && e.getLogo().getDocument() != null) {
            try {
                Image img = Image.getInstance(e.getLogo().getDocument());
                img.scaleToFit(90, 75);
                cell.addElement(img);
            } catch (Exception ignored) {
            }

            if (pdf.isNotBlank(e.getSigle())) {
                Paragraph sigle = new Paragraph(e.getSigle(),
                        new Font(Font.HELVETICA, normalSize, Font.BOLD, ctx.colors().primary()));
                sigle.setAlignment(Element.ALIGN_CENTER);
                cell.addElement(sigle);
            }
        }

        return cell;
    }

    private PdfPCell buildEnterpriseInfoCell(PdfHeaderContext ctx) {
        float titleSize  = ctx.config().getFontSizeTitle().floatValue();
        float normalSize = ctx.config().getFontSizeNormal().floatValue();
        Color primary    = ctx.colors().primary();
        Entreprise e     = ctx.magasin().getEntreprise();

        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(8);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        if (pdf.isNotBlank(e.getRaisonSociale()))
            cell.addElement(centeredParagraph(e.getRaisonSociale(),
                    new Font(Font.HELVETICA, titleSize + 4, Font.BOLD, primary)));

        if (e.getActiviteEconomique() != null && pdf.isNotBlank(e.getActiviteEconomique().getLibelle()))
            cell.addElement(centeredParagraph(e.getActiviteEconomique().getLibelle(),
                    new Font(Font.HELVETICA, normalSize + 2, Font.BOLD, primary)));

        Font infoFont = new Font(Font.HELVETICA, normalSize, Font.BOLD, Color.DARK_GRAY);

        String countryName = e.getCountry() != null ? e.getCountry().getName() : null;
        String addressLine = joinNonBlank(" — ", e.getAdresse(), countryName);
        if (pdf.isNotBlank(addressLine))
            cell.addElement(centeredParagraph(addressLine, infoFont));

        if (pdf.isNotBlank(e.getTelephone())) {
            Font telFont = new Font(Font.HELVETICA, normalSize + 1, Font.BOLD, primary);
            cell.addElement(centeredParagraph(
                    pdf.msg("pdf.label.phone") + " : " + e.getTelephone(), telFont));
        }

        if (pdf.isNotBlank(e.getNinea()))
            cell.addElement(centeredParagraph(pdf.msg("pdf.label.ninea") + " : " + e.getNinea(), infoFont));

        if (pdf.isNotBlank(e.getRccm()))
            cell.addElement(centeredParagraph(pdf.msg("pdf.label.rccm") + " : " + e.getRccm(), infoFont));

        return cell;
    }

    /* ── Document label row ────────────────────────────────────────────── */

    private PdfPTable buildDocumentLabelRow(PdfHeaderContext ctx) {
        float normalSize = ctx.config().getFontSizeNormal().floatValue();

        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell(new Phrase(
                ctx.documentLabel(),
                new Font(Font.HELVETICA, normalSize, Font.BOLD, Color.DARK_GRAY)
        ));
        cell.setPadding(6);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(ctx.colors().primary());
        table.addCell(cell);

        return table;
    }

    /* ── Meta table (NUMÉRO | DATE/HEURE | ÉCHÉANCE | MAGASIN) ─────────── */

    private PdfPTable buildMetaTable(PdfHeaderContext ctx) {
        String echeance = ctx.dateEcheance() != null ? DateHelper.formatDisplay(ctx.dateEcheance()) : "—";

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);

        table.addCell(buildMetaCell(ctx, pdf.msg("pdf.label.numero"), ctx.numeroDoc()));
        table.addCell(buildDateHeureMetaCell(ctx));
        table.addCell(buildMetaCell(ctx, pdf.msg("pdf.label.echeance"), echeance));
        table.addCell(buildMagasinMetaCell(ctx));

        return table;
    }

    private PdfPCell buildDateHeureMetaCell(PdfHeaderContext ctx) {
        float normalSize = ctx.config().getFontSizeNormal().floatValue();
        float smallSize  = ctx.config().getFontSizeSmall().floatValue();

        String date  = ctx.dateDoc()  != null ? DateHelper.formatDisplay(ctx.dateDoc()) : "—";
        String heure = ctx.heureDoc() != null ? ctx.heureDoc().format(TIME_FORMAT) : "—";

        PdfPCell cell = new PdfPCell();
        cell.setPadding(6);
        cell.setBorderColor(PdfColor.BORDER.color());
        cell.setBackgroundColor(ctx.colors().lightBg());
        cell.addElement(new Paragraph(pdf.msg("pdf.label.dateHeure"),
                new Font(Font.HELVETICA, smallSize, Font.BOLD, Color.DARK_GRAY)));
        cell.addElement(new Paragraph(date,
                new Font(Font.HELVETICA, normalSize, Font.BOLD, Color.DARK_GRAY)));
        cell.addElement(new Paragraph(heure,
                new Font(Font.HELVETICA, normalSize, Font.BOLD, Color.DARK_GRAY)));
        return cell;
    }

    private PdfPCell buildMetaCell(PdfHeaderContext ctx, String header, String value) {
        float normalSize = ctx.config().getFontSizeNormal().floatValue();
        float smallSize  = ctx.config().getFontSizeSmall().floatValue();

        PdfPCell cell = new PdfPCell();
        cell.setPadding(6);
        cell.setBorderColor(PdfColor.BORDER.color());
        cell.setBackgroundColor(ctx.colors().lightBg());
        cell.addElement(new Paragraph(header, new Font(Font.HELVETICA, smallSize,  Font.BOLD, Color.DARK_GRAY)));
        cell.addElement(new Paragraph(value,  new Font(Font.HELVETICA, normalSize, Font.BOLD, Color.DARK_GRAY)));
        return cell;
    }

    private PdfPCell buildMagasinMetaCell(PdfHeaderContext ctx) {
        float smallSize = ctx.config().getFontSizeSmall().floatValue();
        Magasin magasin = ctx.magasin();

        PdfPCell cell = new PdfPCell();
        cell.setPadding(6);
        cell.setBorderColor(PdfColor.BORDER.color());
        cell.setBackgroundColor(ctx.colors().lightBg());

        cell.addElement(new Paragraph(pdf.msg("pdf.label.magasin"),
                new Font(Font.HELVETICA, smallSize, Font.BOLD, Color.DARK_GRAY)));

        Font valueFont  = new Font(Font.HELVETICA, smallSize, Font.BOLD,   Color.DARK_GRAY);
        Font detailFont = new Font(Font.HELVETICA, smallSize, Font.NORMAL, Color.DARK_GRAY);

        if (pdf.isNotBlank(magasin.getNom()))
            cell.addElement(new Paragraph(magasin.getNom(), valueFont));
        if (pdf.isNotBlank(magasin.getAdresse()))
            cell.addElement(new Paragraph(magasin.getAdresse(), detailFont));
        if (pdf.isNotBlank(magasin.getTelephone()))
            cell.addElement(new Paragraph(magasin.getTelephone(), detailFont));

        return cell;
    }

    /* ── Client row ────────────────────────────────────────────────────── */

    private PdfPTable buildClientRow(PdfHeaderContext ctx) {
        float smallSize = ctx.config().getFontSizeSmall().floatValue();

        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);

        String text = pdf.msg("pdf.label.client") + " : " + ctx.clientLabel();
        PdfPCell cell = new PdfPCell(new Phrase(text,
                new Font(Font.HELVETICA, smallSize, Font.BOLD, Color.DARK_GRAY)));
        cell.setPadding(6);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(PdfColor.BORDER.color());
        table.addCell(cell);

        return table;
    }
}
