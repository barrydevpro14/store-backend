package org.store.common.pdf.renderer;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
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
 * Classe de base pour les renderers thermiques (80mm, 58mm).
 * Génère le document sur une hauteur maximale, puis le rogne à la hauteur du contenu via PdfStamper.
 * Pas de footer (inutile sur ticket de caisse).
 */
public abstract class AbstractThermalPdfRenderer extends AbstractPdfRenderer {

    private static final float MAX_HEIGHT = 14400f;

    protected AbstractThermalPdfRenderer(IPdfService pdf) {
        super(pdf);
    }

    /**
     * Génère le PDF thermique : contenu sur une page de MAX_HEIGHT,
     * puis CropBox recalculée pour ne montrer que la zone imprimée.
     */
    protected byte[] buildDocument(PdfFormatConfig config, Magasin magasin, DocumentConsumer consumer) {
        float w           = config.getPageWidth().floatValue();
        float marginBottom = config.getMarginBottom().floatValue();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(
                    new Rectangle(w, MAX_HEIGHT),
                    config.getMarginLeft().floatValue(),
                    config.getMarginRight().floatValue(),
                    config.getMarginTop().floatValue(),
                    marginBottom
            );
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            ThermalPositionCapture capture = new ThermalPositionCapture();
            writer.setPageEvent(capture);

            doc.open();
            consumer.accept(doc);
            addSentinel(doc, config);
            doc.close();

            return cropToContent(out.toByteArray(), w, capture.cursorY, marginBottom);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate thermal PDF", e);
        }
    }

    private void addSentinel(Document doc, PdfFormatConfig config) throws DocumentException {
        float size = config.getFontSizeSmall().floatValue();
        Chunk sentinel = new Chunk(" ", new Font(Font.HELVETICA, size, Font.NORMAL, Color.WHITE));
        sentinel.setGenericTag("THERMAL_END");
        doc.add(new Phrase(sentinel));
    }

    private byte[] cropToContent(byte[] source, float w, float cursorY, float marginBottom) {
        float cropBottom = Math.max(0f, cursorY - marginBottom);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfReader reader = new PdfReader(source);
            PdfDictionary page = reader.getPageN(1);
            page.put(PdfName.MEDIABOX, new PdfArray(new float[]{0f, cropBottom, w, MAX_HEIGHT}));
            page.put(PdfName.CROPBOX, new PdfArray(new float[]{0f, cropBottom, w, MAX_HEIGHT}));
            PdfStamper stamper = new PdfStamper(reader, out);
            stamper.close();
            reader.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to crop thermal PDF", e);
        }
    }

    private static final class ThermalPositionCapture extends PdfPageEventHelper {
        float cursorY = 0f;

        @Override
        public void onGenericTag(PdfWriter writer, Document document, Rectangle rect, String text) {
            if ("THERMAL_END".equals(text)) {
                cursorY = rect.getBottom();
            }
        }
    }

    /* ── Header ────────────────────────────────────────────────────────── */

    /**
     * Header compact thermique :
     *   1. Logo (si présent) puis infos entreprise empilées
     *   2. Label du document
     *   3. Tableau méta : NUMÉRO | DATE/HEURE | ÉCHÉANCE | MAGASIN
     *   4. Ligne CLIENT (si non vide)
     */
    protected void addHeader(Document doc, PdfHeaderContext ctx) throws DocumentException {
        doc.add(buildStoreSection(ctx));
        doc.add(buildDocumentLabelRow(ctx));
        doc.add(buildMetaTable(ctx));
        if (pdf.isNotBlank(ctx.clientLabel())) {
            doc.add(buildClientRow(ctx));
        }
    }

    /* ── Store section (empilée : logo puis infos) ─────────────────────── */

    private PdfPTable buildStoreSection(PdfHeaderContext ctx) {
        Entreprise e = ctx.magasin().getEntreprise();
        boolean hasLogo = e.getLogo() != null && e.getLogo().getDocument() != null;

        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);

        if (hasLogo) {
            table.addCell(buildLogoCell(ctx));
        }
        table.addCell(buildEnterpriseInfoCell(ctx));
        return table;
    }

    private PdfPCell buildLogoCell(PdfHeaderContext ctx) {
        Entreprise e = ctx.magasin().getEntreprise();
        float normalSize = ctx.config().getFontSizeNormal().floatValue();

        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(4);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        if (e.getLogo() != null && e.getLogo().getDocument() != null) {
            try {
                Image img = Image.getInstance(e.getLogo().getDocument());
                img.scaleToFit(50, 40);
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
        float smallSize  = ctx.config().getFontSizeSmall().floatValue();
        Color primary    = ctx.colors().primary();
        Entreprise e     = ctx.magasin().getEntreprise();

        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(4);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        if (pdf.isNotBlank(e.getRaisonSociale()))
            cell.addElement(centeredParagraph(e.getRaisonSociale(),
                    new Font(Font.HELVETICA, titleSize, Font.BOLD, primary)));

        if (e.getActiviteEconomique() != null && pdf.isNotBlank(e.getActiviteEconomique().getLibelle()))
            cell.addElement(centeredParagraph(e.getActiviteEconomique().getLibelle(),
                    new Font(Font.HELVETICA, normalSize, Font.BOLD, primary)));

        Font infoFont = new Font(Font.HELVETICA, smallSize, Font.NORMAL, Color.DARK_GRAY);

        if (pdf.isNotBlank(e.getAdresse()))
            cell.addElement(centeredParagraph(e.getAdresse(), infoFont));
        if (pdf.isNotBlank(e.getTelephone()))
            cell.addElement(centeredParagraph(e.getTelephone(), infoFont));
        if (e.getCountry() != null && pdf.isNotBlank(e.getCountry().getName()))
            cell.addElement(centeredParagraph(e.getCountry().getName(), infoFont));
        if (pdf.isNotBlank(e.getNinea()))
            cell.addElement(centeredParagraph(pdf.msg("pdf.label.ninea") + " : " + e.getNinea(), infoFont));
        if (pdf.isNotBlank(e.getRccm()))
            cell.addElement(centeredParagraph(pdf.msg("pdf.label.rccm") + " : " + e.getRccm(), infoFont));

        return cell;
    }

    /* ── Document label row ────────────────────────────────────────────── */

    private PdfPTable buildDocumentLabelRow(PdfHeaderContext ctx) {
        float smallSize = ctx.config().getFontSizeSmall().floatValue();

        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell(new Phrase(
                ctx.documentLabel(),
                new Font(Font.HELVETICA, smallSize, Font.BOLD, Color.DARK_GRAY)
        ));
        cell.setPadding(4);
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
        float smallSize = ctx.config().getFontSizeSmall().floatValue();

        String date  = ctx.dateDoc()  != null ? DateHelper.formatDisplay(ctx.dateDoc()) : "—";
        String heure = ctx.heureDoc() != null ? ctx.heureDoc().format(TIME_FORMAT) : "—";

        PdfPCell cell = new PdfPCell();
        cell.setPadding(4);
        cell.setBorderColor(PdfColor.BORDER.color());
        cell.setBackgroundColor(ctx.colors().lightBg());
        cell.addElement(new Paragraph(pdf.msg("pdf.label.dateHeure"),
                new Font(Font.HELVETICA, smallSize, Font.BOLD, Color.DARK_GRAY)));
        cell.addElement(new Paragraph(date,
                new Font(Font.HELVETICA, smallSize, Font.BOLD, Color.DARK_GRAY)));
        cell.addElement(new Paragraph(heure,
                new Font(Font.HELVETICA, smallSize, Font.BOLD, Color.DARK_GRAY)));
        return cell;
    }

    private PdfPCell buildMetaCell(PdfHeaderContext ctx, String header, String value) {
        float smallSize = ctx.config().getFontSizeSmall().floatValue();

        PdfPCell cell = new PdfPCell();
        cell.setPadding(4);
        cell.setBorderColor(PdfColor.BORDER.color());
        cell.setBackgroundColor(ctx.colors().lightBg());
        cell.addElement(new Paragraph(header, new Font(Font.HELVETICA, smallSize, Font.BOLD,   Color.DARK_GRAY)));
        cell.addElement(new Paragraph(value,  new Font(Font.HELVETICA, smallSize, Font.NORMAL, Color.DARK_GRAY)));
        return cell;
    }

    private PdfPCell buildMagasinMetaCell(PdfHeaderContext ctx) {
        float smallSize = ctx.config().getFontSizeSmall().floatValue();
        Magasin magasin = ctx.magasin();

        PdfPCell cell = new PdfPCell();
        cell.setPadding(4);
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
        cell.setPadding(4);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(PdfColor.BORDER.color());
        table.addCell(cell);

        return table;
    }
}
