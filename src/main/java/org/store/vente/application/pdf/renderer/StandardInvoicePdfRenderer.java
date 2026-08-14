package org.store.vente.application.pdf.renderer;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import org.springframework.stereotype.Service;
import org.store.common.dto.PdfColor;
import org.store.common.dto.PdfColors;
import org.store.common.pdf.PdfHeaderContext;
import org.store.common.pdf.renderer.AbstractStandardPdfRenderer;
import org.store.common.service.IPdfService;
import org.store.common.tools.DateHelper;
import org.store.entreprise.application.service.IEntrepriseSettingService;
import org.store.magasin.domain.model.Magasin;
import org.store.pdf.domain.model.PdfFormatConfig;
import org.store.vente.application.dto.PaiementVenteResponse;
import org.store.vente.application.service.IPaiementVenteService;
import org.store.vente.domain.enums.LivraisonStatut;
import org.store.vente.domain.model.FactureClient;
import org.store.vente.domain.model.LigneCommandeVente;

import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Génère le PDF de facture client pour les formats standard (A4, A5).
 * Header : AbstractPdfRenderer.addHeader. Table lignes et totaux inchangés.
 */
@Service
public class StandardInvoicePdfRenderer extends AbstractStandardPdfRenderer {

    private final IPaiementVenteService paiementVenteService;
    private final IEntrepriseSettingService entrepriseSettingService;

    public StandardInvoicePdfRenderer(IPdfService pdf,
                                       IPaiementVenteService paiementVenteService,
                                       IEntrepriseSettingService entrepriseSettingService) {
        super(pdf);
        this.paiementVenteService = paiementVenteService;
        this.entrepriseSettingService = entrepriseSettingService;
    }

    public byte[] render(FactureClient facture, Magasin magasin, PdfFormatConfig config) {
        PdfColors colors = pdf.resolveColors(entrepriseSettingService.getMySettings().couleurPrimaire());
        LocalDateTime now = LocalDateTime.now();

        PdfHeaderContext ctx = new PdfHeaderContext(
                magasin,
                facture.getNumero(),
                now.toLocalDate(),
                now.toLocalTime(),
                facture.getDateEcheance(),
                buildClientLabel(facture),
                pdf.msg("pdf.vente.title"),
                colors,
                config
        );

        return buildDocument(config, magasin, doc -> {
            addHeader(doc, ctx);
            doc.add(Chunk.NEWLINE);
            addLinesTable(doc, facture, ctx);
            doc.add(Chunk.NEWLINE);
            addTotalsAndPayments(doc, facture, ctx);
        });
    }

    /* ── Client label ──────────────────────────────────────────────────── */

    private String buildClientLabel(FactureClient facture) {
        var client = facture.getCommande().getClient();
        if (client == null) return pdf.msg("pdf.vente.client.anonyme");
        String identity = joinNonBlank(" ", client.getNom(), client.getPrenom());
        return joinNonBlank(" / ", identity, client.getTelephone());
    }

    /* ── Lines table ───────────────────────────────────────────────────── */

    private void addLinesTable(Document doc, FactureClient facture, PdfHeaderContext ctx) throws DocumentException {
        float smallSize = ctx.config().getFontSizeSmall().floatValue();
        PdfColors colors = ctx.colors();

        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{25, 18, 7, 8, 16, 13, 13});

        Font headFont = new Font(Font.HELVETICA, smallSize, Font.BOLD, Color.WHITE);
        String[] headers = {
            pdf.msg("pdf.vente.table.produit"),
            pdf.msg("pdf.table.categorieQualite"),
            pdf.msg("pdf.vente.table.quantite"),
            pdf.msg("pdf.vente.table.quantiteLivree"),
            pdf.msg("pdf.vente.table.livraisonStatut"),
            pdf.msg("pdf.vente.table.prixUnitaire"),
            pdf.msg("pdf.vente.table.totalHt")
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

        for (LigneCommandeVente ligne : facture.getCommande().getLignes()) {
            Color bg = alt ? new Color(249, 250, 251) : Color.WHITE;
            alt = !alt;
            addLigneRow(table, ligne, lineFont, bg);
        }

        doc.add(table);
    }

    private void addLigneRow(PdfPTable table, LigneCommandeVente ligne, Font lineFont, Color bg) {
        var product = ligne.getProductFournisseur().getProduct();
        var quality = ligne.getProductFournisseur().getQuality();
        String symbole = product.getUniteMesure().getSymbole();

        table.addCell(pdf.textCell(buildProductLabel(product.getNom(), product.getReference()), lineFont, bg));
        table.addCell(pdf.textCell(buildCategoryQualityLabel(product.getCategoryProduct(), quality), lineFont, bg));
        table.addCell(pdf.numCell(ligne.getQuantite() + " " + symbole, lineFont, bg));
        table.addCell(pdf.numCell(ligne.getQuantiteLivree() + " " + symbole, lineFont, bg));
        table.addCell(buildLivraisonStatutCell(ligne.getLivraisonStatut(), lineFont));
        table.addCell(pdf.numCell(pdf.formatAmount(ligne.getPrixUnitaire()), lineFont, bg));
        table.addCell(pdf.numCell(pdf.formatAmount(ligne.getMontantTotal()), lineFont, bg));
    }

    private PdfPCell buildLivraisonStatutCell(LivraisonStatut statut, Font baseFont) {
        String label = statut != null ? pdf.msg("pdf.vente.livraison." + statut.name()) : "—";
        Font font = new Font(baseFont.getFamily(), baseFont.getSize(), Font.BOLD, Color.DARK_GRAY);
        PdfPCell cell = new PdfPCell(new Phrase(label, font));
        cell.setBackgroundColor(livraisonStatutBackground(statut));
        cell.setPadding(5);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    private Color livraisonStatutBackground(LivraisonStatut statut) {
        if (statut == null) return Color.WHITE;
        return switch (statut) {
            case LIVREE               -> new Color(220, 252, 231);
            case NON_LIVREE           -> new Color(254, 226, 226);
            case PARTIELLEMENT_LIVREE -> new Color(254, 249, 195);
        };
    }

    /* ── Totals & payments ─────────────────────────────────────────────── */

    private void addTotalsAndPayments(Document doc, FactureClient facture, PdfHeaderContext ctx) throws DocumentException {
        float normalSize = ctx.config().getFontSizeNormal().floatValue();
        PdfColors colors = ctx.colors();

        PdfPTable totals = new PdfPTable(2);
        totals.setWidths(new float[]{65, 35});
        totals.setWidthPercentage(100);

        Font labelFont = new Font(Font.HELVETICA, normalSize, Font.NORMAL, PdfColor.GRAY_TEXT.color());
        Font valueFont = new Font(Font.HELVETICA, normalSize, Font.NORMAL, Color.DARK_GRAY);
        Font boldFont  = new Font(Font.HELVETICA, normalSize + 2, Font.BOLD, colors.primary());

        pdf.addTotalRow(totals, pdf.msg("pdf.vente.totals.totalHt"), pdf.formatAmount(facture.getMontantTotal()), labelFont, valueFont, Color.WHITE);

        for (PaiementVenteResponse p : paiementVenteService.findAllByFactureId(facture.getId())) {
            String label = pdf.msg("pdf.vente.totals.paiement") + " (" + (p.moyen() != null ? p.moyen().libelle() : "—") + ")";
            if (p.datePaiement() != null) label += " – " + DateHelper.formatDisplay(p.datePaiement());
            pdf.addTotalRow(totals, label, pdf.formatAmount(p.montant()), labelFont, valueFont, Color.WHITE);
        }

        BigDecimal reste = facture.getMontantTotal().subtract(facture.getMontantPaye());
        Color resteBg = reste.compareTo(BigDecimal.ZERO) == 0
                ? new Color(236, 253, 245) : new Color(255, 251, 235);
        pdf.addTotalRow(totals, pdf.msg("pdf.vente.totals.soldeRestant"), pdf.formatAmount(reste), boldFont, boldFont, resteBg);

        doc.add(totals);
    }
}
