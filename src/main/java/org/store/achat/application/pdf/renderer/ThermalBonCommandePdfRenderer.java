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
import org.store.common.pdf.renderer.AbstractThermalPdfRenderer;
import org.store.common.service.IPdfService;
import org.store.entreprise.application.service.IEntrepriseSettingService;
import org.store.magasin.domain.model.Magasin;
import org.store.pdf.domain.model.PdfFormatConfig;

import java.awt.*;
import java.time.LocalDateTime;

/**
 * Génère le PDF bon de commande achat pour les formats thermiques (80mm, 58mm).
 * Layout compact sans tableau multi-colonnes.
 */
@Service
public class ThermalBonCommandePdfRenderer extends AbstractThermalPdfRenderer {

    private final IEntrepriseSettingService entrepriseSettingService;

    public ThermalBonCommandePdfRenderer(IPdfService pdf,
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
            addSeparator(doc, ctx);
            addLignesCompact(doc, commande, ctx);
            addSeparator(doc, ctx);
            addTotalCompact(doc, commande, ctx);
        });
    }

    /* ── Fournisseur label ─────────────────────────────────────────────── */

    private String buildFournisseurLabel(CommandeAchat commande) {
        if (commande.getFournisseur() == null) return "";
        var f = commande.getFournisseur();
        return joinNonBlank(" ", f.getNom(), f.getPrenom());
    }

    /* ── Separator ─────────────────────────────────────────────────────── */

    private void addSeparator(Document doc, PdfHeaderContext ctx) throws DocumentException {
        float smallSize = ctx.config().getFontSizeSmall().floatValue();
        Font sepFont = new Font(Font.COURIER, smallSize, Font.NORMAL, Color.GRAY);
        doc.add(new Paragraph("--------------------------------", sepFont));
    }

    /* ── Compact lines ─────────────────────────────────────────────────── */

    private void addLignesCompact(Document doc, CommandeAchat commande, PdfHeaderContext ctx) throws DocumentException {
        float smallSize = ctx.config().getFontSizeSmall().floatValue();

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{70, 30});

        for (LigneCommandeAchat ligne : commande.getLignes()) {
            addLigneCompactRow(table, ligne, smallSize);
        }

        doc.add(table);
    }

    private void addLigneCompactRow(PdfPTable table, LigneCommandeAchat ligne, float smallSize) {
        var product = ligne.getProductFournisseur().getProduct();
        String symbole = product.getUniteMesure().getSymbole();
        String desc = buildProductLabel(product.getNom(), product.getReference())
                + "\n" + ligne.getQuantite() + " " + symbole
                + " × " + pdf.formatAmount(ligne.getPrixAchat());

        Font lineFont = new Font(Font.COURIER, smallSize, Font.NORMAL, Color.DARK_GRAY);

        PdfPCell descCell = new PdfPCell(new Phrase(desc, lineFont));
        descCell.setBorder(Rectangle.NO_BORDER);
        descCell.setPadding(3);
        table.addCell(descCell);

        PdfPCell amountCell = new PdfPCell(new Phrase(pdf.formatAmount(ligne.getPrixAchat()), lineFont));
        amountCell.setBorder(Rectangle.NO_BORDER);
        amountCell.setPadding(3);
        amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(amountCell);
    }

    /* ── Compact total ─────────────────────────────────────────────────── */

    private void addTotalCompact(Document doc, CommandeAchat commande, PdfHeaderContext ctx) throws DocumentException {
        float normalSize = ctx.config().getFontSizeNormal().floatValue();
        PdfColors colors = ctx.colors();

        PdfPTable totals = new PdfPTable(2);
        totals.setWidthPercentage(100);
        totals.setWidths(new float[]{60, 40});

        Font boldFont = new Font(Font.COURIER, normalSize, Font.BOLD, colors.primary());

        PdfPCell lc = new PdfPCell(new Phrase(pdf.msg("pdf.achat.total"), boldFont));
        lc.setBorder(Rectangle.NO_BORDER);
        lc.setPadding(3);
        totals.addCell(lc);

        PdfPCell vc = new PdfPCell(new Phrase(pdf.formatAmount(commande.getMontantTotal()), boldFont));
        vc.setBorder(Rectangle.NO_BORDER);
        vc.setPadding(3);
        vc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totals.addCell(vc);

        doc.add(totals);
    }
}
