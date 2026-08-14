package org.store.achat.application.pdf.renderer;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import org.springframework.stereotype.Service;
import org.store.achat.domain.model.CommandeAchat;
import org.store.achat.domain.model.LigneCommandeAchat;
import org.store.common.dto.PdfColors;
import org.store.common.pdf.PdfHeaderContext;
import org.store.common.pdf.renderer.AbstractStandardPdfRenderer;
import org.store.common.service.IPdfService;
import org.store.entreprise.application.service.IEntrepriseSettingService;
import org.store.magasin.domain.model.Magasin;
import org.store.pdf.domain.model.PdfFormatConfig;

import java.awt.*;
import java.time.LocalDateTime;

/**
 * Génère le PDF bon de commande achat pour les formats standard (A4, A5).
 * Header : AbstractPdfRenderer.addHeader. Table lignes et total inchangés.
 */
@Service
public class StandardBonCommandePdfRenderer extends AbstractStandardPdfRenderer {

    private final IEntrepriseSettingService entrepriseSettingService;

    public StandardBonCommandePdfRenderer(IPdfService pdf,
                                           IEntrepriseSettingService entrepriseSettingService) {
        super(pdf);
        this.entrepriseSettingService = entrepriseSettingService;
    }

    public byte[] render(CommandeAchat commande, Magasin magasin, PdfFormatConfig config) {
        PdfColors colors = pdf.resolveColors(entrepriseSettingService.getMySettings().couleurPrimaire());
        LocalDateTime now = LocalDateTime.now();

        PdfHeaderContext ctx = new PdfHeaderContext(
                magasin,
                commande.getReference(),
                now.toLocalDate(),
                now.toLocalTime(),
                null,
                buildFournisseurLabel(commande),
                pdf.msg("pdf.achat.title"),
                colors,
                config
        );

        return buildDocument(config, magasin, doc -> {
            addHeader(doc, ctx);
            doc.add(Chunk.NEWLINE);
            addLinesTable(doc, commande, ctx);
            doc.add(Chunk.NEWLINE);
            addTotal(doc, commande, ctx);
        });
    }

    /* ── Fournisseur label ─────────────────────────────────────────────── */

    private String buildFournisseurLabel(CommandeAchat commande) {
        if (commande.getFournisseur() == null) return "";
        var f = commande.getFournisseur();
        return joinNonBlank(" ", f.getNom(), f.getPrenom());
    }

    /* ── Lines table ───────────────────────────────────────────────────── */

    private void addLinesTable(Document doc, CommandeAchat commande, PdfHeaderContext ctx) throws DocumentException {
        float smallSize = ctx.config().getFontSizeSmall().floatValue();
        PdfColors colors = ctx.colors();

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{35, 30, 12, 23});

        Font headFont = new Font(Font.HELVETICA, smallSize, Font.BOLD, Color.WHITE);
        String[] headers = {
            pdf.msg("pdf.achat.table.produit"),
            pdf.msg("pdf.table.categorieQualite"),
            pdf.msg("pdf.achat.table.quantite"),
            pdf.msg("pdf.achat.table.prixAchat")
        };

        for (int i = 0; i < headers.length; i++) {
            PdfPCell cell = new PdfPCell(new Phrase(headers[i], headFont));
            cell.setBackgroundColor(colors.primary());
            cell.setPadding(7);
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setHorizontalAlignment(i <= 1 ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT);
            table.addCell(cell);
        }

        Font lineFont = new Font(Font.HELVETICA, smallSize, Font.NORMAL, Color.DARK_GRAY);
        boolean alt = false;

        for (LigneCommandeAchat ligne : commande.getLignes()) {
            Color bg = alt ? new Color(249, 250, 251) : Color.WHITE;
            alt = !alt;
            addLigneRow(table, ligne, lineFont, bg);
        }

        doc.add(table);
    }

    private void addLigneRow(PdfPTable table, LigneCommandeAchat ligne, Font lineFont, Color bg) {
        var product = ligne.getProductFournisseur().getProduct();
        var quality = ligne.getProductFournisseur().getQuality();
        String symbole = product.getUniteMesure().getSymbole();

        table.addCell(pdf.textCell(buildProductLabel(product.getNom(), product.getReference()), lineFont, bg));
        table.addCell(pdf.textCell(buildCategoryQualityLabel(product.getCategoryProduct(), quality), lineFont, bg));
        table.addCell(pdf.numCell(ligne.getQuantite() + " " + symbole, lineFont, bg));
        table.addCell(pdf.numCell(pdf.formatAmount(ligne.getPrixAchat()), lineFont, bg));
    }

    /* ── Total ─────────────────────────────────────────────────────────── */

    private void addTotal(Document doc, CommandeAchat commande, PdfHeaderContext ctx) throws DocumentException {
        float normalSize = ctx.config().getFontSizeNormal().floatValue();
        PdfColors colors = ctx.colors();

        PdfPTable totals = new PdfPTable(2);
        totals.setWidths(new float[]{65, 35});
        totals.setWidthPercentage(100);

        Font boldFont = new Font(Font.HELVETICA, normalSize + 2, Font.BOLD, colors.primary());
        pdf.addTotalRow(totals, pdf.msg("pdf.achat.total"), pdf.formatAmount(commande.getMontantTotal()), boldFont, boldFont, colors.lightBg());

        doc.add(totals);
    }
}
