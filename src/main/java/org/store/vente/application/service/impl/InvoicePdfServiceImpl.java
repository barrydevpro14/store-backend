package org.store.vente.application.service.impl;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.service.IPdfService;
import org.store.common.dto.PdfColor;
import org.store.common.dto.PdfColors;
import org.store.common.tools.DateHelper;
import org.store.common.tools.OwnershipHelper;
import org.store.entreprise.application.service.IEntrepriseSettingService;
import org.store.magasin.domain.model.Magasin;
import org.store.produit.domain.model.CategoryProduct;
import org.store.produit.domain.model.Quality;
import org.store.security.application.service.ICurrentUserService;
import org.store.vente.application.dto.PaiementVenteResponse;
import org.store.vente.application.service.IFactureClientService;
import org.store.vente.application.service.IInvoicePdfService;
import org.store.vente.application.service.IPaiementVenteService;
import org.store.vente.domain.enums.LivraisonStatut;
import org.store.vente.domain.model.FactureClient;
import org.store.vente.domain.model.LigneCommandeVente;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Generates a PDF invoice for a client sale using OpenPDF.
 * Layout: store header / invoice metadata / lines table / totals / payments.
 */
@Service
@Transactional(readOnly = true)
public class InvoicePdfServiceImpl implements IInvoicePdfService {

    private final IFactureClientService factureClientService;
    private final IPaiementVenteService paiementVenteService;
    private final ICurrentUserService currentUserService;
    private final IPdfService pdf;
    private final IEntrepriseSettingService entrepriseSettingService;

    public InvoicePdfServiceImpl(IFactureClientService factureClientService,
                                  IPaiementVenteService paiementVenteService,
                                  ICurrentUserService currentUserService,
                                  IPdfService pdf,
                                  IEntrepriseSettingService entrepriseSettingService) {
        this.factureClientService = factureClientService;
        this.paiementVenteService = paiementVenteService;
        this.currentUserService = currentUserService;
        this.pdf = pdf;
        this.entrepriseSettingService = entrepriseSettingService;
    }

    @Override
    public byte[] generate(UUID factureId) {
        FactureClient facture = factureClientService.findById(factureId);

        OwnershipHelper.ensureOwnership(
                facture,
                facture.getCommande().getMagasin().getEntreprise().getId(),
                currentUserService.getCurrent().entrepriseId(),
                "factureClient.notOwned"
        );

        Magasin magasin = facture.getCommande().getMagasin();
        PdfColors colors = resolveColors();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 40, 40, 40, 120);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            pdf.configureFooter(writer, magasin);
            doc.open();

            addHeader(doc, magasin, facture, colors);
            doc.add(Chunk.NEWLINE);
            addClientAndMeta(doc, facture);
            doc.add(Chunk.NEWLINE);
            addLinesTable(doc, facture, colors);
            doc.add(Chunk.NEWLINE);
            addTotalsAndPayments(doc, facture, colors);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate invoice PDF", e);
        }
    }

    private PdfColors resolveColors() {
        return pdf.resolveColors(entrepriseSettingService.getMySettings().couleurPrimaire());
    }

    /* ── Header ────────────────────────────────────────────────────────── */

    private void addHeader(Document doc, Magasin magasin, FactureClient facture, PdfColors colors) throws DocumentException {
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{55, 45});

        header.addCell(pdf.buildStoreCell(magasin, colors));

        PdfPCell invoiceCell = new PdfPCell();
        invoiceCell.setBorder(Rectangle.NO_BORDER);
        invoiceCell.setBackgroundColor(colors.primary());
        invoiceCell.setPadding(12);
        invoiceCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        Font titleFont = new Font(Font.HELVETICA, 20, Font.BOLD, Color.WHITE);
        Font numFont   = new Font(Font.HELVETICA, 11, Font.BOLD, Color.WHITE);
        Font dateFont  = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.WHITE);

        invoiceCell.addElement(new Paragraph(pdf.msg("pdf.vente.title"), titleFont));
        invoiceCell.addElement(new Paragraph(facture.getNumero(), numFont));
        if (facture.getDate() != null)
            invoiceCell.addElement(new Paragraph(pdf.msg("pdf.label.date") + " : " + DateHelper.formatDisplay(facture.getDate()), dateFont));
        if (facture.getDateEcheance() != null)
            invoiceCell.addElement(new Paragraph(pdf.msg("pdf.label.echeance") + " : " + DateHelper.formatDisplay(facture.getDateEcheance()), dateFont));

        header.addCell(invoiceCell);
        doc.add(header);
    }

    /* ── Client & meta ─────────────────────────────────────────────────── */

    private void addClientAndMeta(Document doc, FactureClient facture) throws DocumentException {
        Font valueFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.DARK_GRAY);
        Font infoFont  = new Font(Font.HELVETICA, 9, Font.NORMAL, PdfColor.GRAY_TEXT.color());

        PdfPCell clientCell = pdf.sectionCell(pdf.msg("pdf.vente.section.client"));
        if (facture.getCommande().getClient() != null) {
            var client = facture.getCommande().getClient();
            String identityLine = joinNonBlank(" ", client.getNom(), client.getPrenom());
            String contactLine  = joinNonBlank(" / ", client.getTelephone(), client.getEmail(), client.getAdresse());

            if (pdf.isNotBlank(identityLine))
                clientCell.addElement(new Paragraph(identityLine, valueFont));
            if (pdf.isNotBlank(contactLine))
                clientCell.addElement(new Paragraph(contactLine, infoFont));
        } else {
            clientCell.addElement(new Paragraph(pdf.msg("pdf.vente.client.anonyme"), new Font(Font.HELVETICA, 9, Font.ITALIC, PdfColor.GRAY_TEXT.color())));
        }

        PdfPTable meta = new PdfPTable(1);
        meta.setWidthPercentage(50);
        meta.setHorizontalAlignment(Element.ALIGN_LEFT);
        meta.addCell(clientCell);
        doc.add(meta);
    }

    private String joinNonBlank(String separator, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!pdf.isNotBlank(part)) continue;
            if (sb.length() > 0) sb.append(separator);
            sb.append(part);
        }
        return sb.toString();
    }

    /* ── Lines table ───────────────────────────────────────────────────── */

    private void addLinesTable(Document doc, FactureClient facture, PdfColors colors) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{25, 18, 7, 8, 16, 13, 13});
        table.setWidthPercentage(100);

        Font headFont = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
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

        Font lineFont = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);
        boolean alt   = false;

        for (LigneCommandeVente ligne : facture.getCommande().getLignes()) {
            Color bg = alt ? new Color(249, 250, 251) : Color.WHITE;
            alt = !alt;

            var product = ligne.getProductFournisseur().getProduct();
            var quality = ligne.getProductFournisseur().getQuality();

            table.addCell(pdf.textCell(buildProductLabel(product.getNom(), product.getReference()), lineFont, bg));
            table.addCell(pdf.textCell(buildCategoryQualityLabel(product.getCategoryProduct(), quality), lineFont, bg));
            table.addCell(pdf.numCell(String.valueOf(ligne.getQuantite()), lineFont, bg));
            table.addCell(pdf.numCell(String.valueOf(ligne.getQuantiteLivree()), lineFont, bg));
            table.addCell(buildLivraisonStatutCell(ligne.getLivraisonStatut(), lineFont));
            table.addCell(pdf.numCell(pdf.formatAmount(ligne.getPrixUnitaire()), lineFont, bg));
            table.addCell(pdf.numCell(pdf.formatAmount(ligne.getMontantTotal()), lineFont, bg));
        }

        doc.add(table);
    }

    private PdfPCell buildLivraisonStatutCell(LivraisonStatut statut, Font baseFont) {
        String label = statut != null ? pdf.msg("pdf.vente.livraison." + statut.name()) : "—";
        Color bg = livraisonStatutBackground(statut);

        Font font = new Font(baseFont.getFamily(), baseFont.getSize(), Font.BOLD, Color.DARK_GRAY);
        PdfPCell cell = new PdfPCell(new Phrase(label, font));
        cell.setBackgroundColor(bg);
        cell.setPadding(5);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    private Color livraisonStatutBackground(LivraisonStatut statut) {
        if (statut == null) return Color.WHITE;
        return switch (statut) {
            case LIVREE                -> new Color(220, 252, 231);
            case NON_LIVREE            -> new Color(254, 226, 226);
            case PARTIELLEMENT_LIVREE  -> new Color(254, 249, 195);
        };
    }

    private String buildProductLabel(String nom, String ref) {
        return pdf.isNotBlank(ref) ? nom + "(" + ref + ")" : pdf.nullToEmpty(nom);
    }

    private String buildCategoryQualityLabel(CategoryProduct category, Quality quality) {
        String categoryLabel = category != null && pdf.isNotBlank(category.getLibelle()) ? category.getLibelle() : null;
        String qualityLabel  = quality != null && pdf.isNotBlank(quality.getLibelle()) ? quality.getLibelle() : null;

        if (categoryLabel != null && qualityLabel != null) return categoryLabel + " / " + qualityLabel;
        if (categoryLabel != null) return categoryLabel;
        if (qualityLabel != null) return qualityLabel;
        return "—";
    }

    /* ── Totals & payments ─────────────────────────────────────────────── */

    private void addTotalsAndPayments(Document doc, FactureClient facture, PdfColors colors) throws DocumentException {
        PdfPTable totals = new PdfPTable(new float[]{65, 35});
        totals.setWidthPercentage(100);

        Font labelFont = new Font(Font.HELVETICA, 9, Font.NORMAL, PdfColor.GRAY_TEXT.color());
        Font valueFont = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);
        Font boldFont  = new Font(Font.HELVETICA, 11, Font.BOLD, colors.primary());

        pdf.addTotalRow(totals, pdf.msg("pdf.vente.totals.totalHt"), pdf.formatAmount(facture.getMontantTotal()), labelFont, valueFont, Color.WHITE);

        var paiements = paiementVenteService.findAllByFactureId(facture.getId());
        for (PaiementVenteResponse p : paiements) {
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
