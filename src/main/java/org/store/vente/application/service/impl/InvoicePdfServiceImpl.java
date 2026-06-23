package org.store.vente.application.service.impl;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.service.IPdfService;
import org.store.common.tools.OwnershipHelper;
import org.store.magasin.domain.model.Magasin;
import org.store.security.application.service.ICurrentUserService;
import org.store.vente.application.service.IInvoicePdfService;
import org.store.vente.domain.model.FactureClient;
import org.store.vente.domain.model.LigneCommandeVente;
import org.store.vente.domain.service.FactureClientDomainService;
import org.store.vente.domain.service.PaiementVenteDomainService;

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

    private final FactureClientDomainService factureClientDomainService;
    private final PaiementVenteDomainService paiementVenteDomainService;
    private final ICurrentUserService currentUserService;
    private final IPdfService pdf;

    public InvoicePdfServiceImpl(FactureClientDomainService factureClientDomainService,
                                  PaiementVenteDomainService paiementVenteDomainService,
                                  ICurrentUserService currentUserService,
                                  IPdfService pdf) {
        this.factureClientDomainService = factureClientDomainService;
        this.paiementVenteDomainService = paiementVenteDomainService;
        this.currentUserService = currentUserService;
        this.pdf = pdf;
    }

    @Override
    public byte[] generate(UUID factureId) {
        FactureClient facture = factureClientDomainService.findById(factureId);

        OwnershipHelper.ensureOwnership(
                facture,
                facture.getCommande().getMagasin().getEntreprise().getId(),
                currentUserService.getCurrent().entrepriseId(),
                "factureClient.notOwned"
        );

        Magasin magasin = facture.getCommande().getMagasin();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter.getInstance(doc, out);
            doc.open();

            addHeader(doc, magasin, facture);
            doc.add(Chunk.NEWLINE);
            addClientAndMeta(doc, facture);
            doc.add(Chunk.NEWLINE);
            addLinesTable(doc, facture);
            doc.add(Chunk.NEWLINE);
            addTotalsAndPayments(doc, facture);
            pdf.addFooter(doc, magasin);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate invoice PDF", e);
        }
    }

    /* ── Header ────────────────────────────────────────────────────────── */

    private void addHeader(Document doc, Magasin magasin, FactureClient facture) throws DocumentException {
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{55, 45});

        header.addCell(pdf.buildStoreCell(magasin));

        PdfPCell invoiceCell = new PdfPCell();
        invoiceCell.setBorder(Rectangle.NO_BORDER);
        invoiceCell.setBackgroundColor(IPdfService.LIGHT_BG);
        invoiceCell.setPadding(12);
        invoiceCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        Font titleFont = new Font(Font.HELVETICA, 20, Font.BOLD, IPdfService.PRIMARY);
        Font numFont   = new Font(Font.HELVETICA, 11, Font.BOLD, Color.DARK_GRAY);
        Font dateFont  = new Font(Font.HELVETICA, 9, Font.NORMAL, IPdfService.GRAY_TEXT);

        invoiceCell.addElement(new Paragraph(pdf.msg("pdf.vente.title"), titleFont));
        invoiceCell.addElement(new Paragraph(facture.getNumero(), numFont));
        if (facture.getDate() != null)
            invoiceCell.addElement(new Paragraph(pdf.msg("pdf.label.date") + " : " + facture.getDate().format(IPdfService.DATE_FMT), dateFont));
        if (facture.getDateEcheance() != null)
            invoiceCell.addElement(new Paragraph(pdf.msg("pdf.label.echeance") + " : " + facture.getDateEcheance().format(IPdfService.DATE_FMT), dateFont));

        header.addCell(invoiceCell);
        doc.add(header);
    }

    /* ── Client & meta ─────────────────────────────────────────────────── */

    private void addClientAndMeta(Document doc, FactureClient facture) throws DocumentException {
        Font valueFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.DARK_GRAY);

        PdfPCell clientCell = pdf.sectionCell(pdf.msg("pdf.vente.section.client"));
        if (facture.getCommande().getClient() != null) {
            var client = facture.getCommande().getClient();
            clientCell.addElement(new Paragraph(client.getNom() + " " + pdf.nullToEmpty(client.getPrenom()), valueFont));
            if (pdf.isNotBlank(client.getTelephone()))
                clientCell.addElement(new Paragraph(client.getTelephone(), new Font(Font.HELVETICA, 9, Font.NORMAL, IPdfService.GRAY_TEXT)));
            if (pdf.isNotBlank(client.getEmail()))
                clientCell.addElement(new Paragraph(client.getEmail(), new Font(Font.HELVETICA, 9, Font.NORMAL, IPdfService.GRAY_TEXT)));
        } else {
            clientCell.addElement(new Paragraph(pdf.msg("pdf.vente.client.anonyme"), new Font(Font.HELVETICA, 9, Font.ITALIC, IPdfService.GRAY_TEXT)));
        }

        PdfPTable meta = new PdfPTable(1);
        meta.setWidthPercentage(50);
        meta.setHorizontalAlignment(Element.ALIGN_LEFT);
        meta.addCell(clientCell);
        doc.add(meta);
    }

    /* ── Lines table ───────────────────────────────────────────────────── */

    private void addLinesTable(Document doc, FactureClient facture) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{35, 15, 25, 25});
        table.setWidthPercentage(100);

        Font headFont = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
        String[] headers = {
            pdf.msg("pdf.vente.table.produit"),
            pdf.msg("pdf.vente.table.quantite"),
            pdf.msg("pdf.vente.table.prixUnitaire"),
            pdf.msg("pdf.vente.table.totalHt")
        };
        for (int i = 0; i < headers.length; i++) {
            PdfPCell cell = new PdfPCell(new Phrase(headers[i], headFont));
            cell.setBackgroundColor(IPdfService.PRIMARY);
            cell.setPadding(7);
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setHorizontalAlignment(i == 0 ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT);
            table.addCell(cell);
        }

        Font lineFont = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);
        Font refFont  = new Font(Font.HELVETICA, 8, Font.NORMAL, IPdfService.GRAY_TEXT);
        boolean alt   = false;

        for (LigneCommandeVente ligne : facture.getCommande().getLignes()) {
            Color bg = alt ? new Color(249, 250, 251) : Color.WHITE;
            alt = !alt;

            PdfPCell nameCell = new PdfPCell();
            nameCell.setBackgroundColor(bg);
            nameCell.setBorderColor(IPdfService.BORDER);
            nameCell.setPadding(6);
            var product = ligne.getProductFournisseur().getProduct();
            String nom = product.getNom();
            String ref = product.getReference();
            nameCell.addElement(new Paragraph(nom, lineFont));
            if (pdf.isNotBlank(ref))
                nameCell.addElement(new Paragraph(pdf.msg("pdf.label.refShort") + ": " + ref, refFont));
            var category = product.getCategoryProduct();
            if (category != null && pdf.isNotBlank(category.getLibelle()))
                nameCell.addElement(new Paragraph(pdf.msg("pdf.label.category") + " : " + category.getLibelle(), refFont));
            var quality = ligne.getProductFournisseur().getQuality();
            if (quality != null && pdf.isNotBlank(quality.getLibelle()))
                nameCell.addElement(new Paragraph(pdf.msg("pdf.label.qualite") + " : " + quality.getLibelle(), refFont));
            table.addCell(nameCell);

            table.addCell(pdf.numCell(String.valueOf(ligne.getQuantite()), lineFont, bg));
            table.addCell(pdf.numCell(pdf.formatAmount(ligne.getPrixUnitaire()), lineFont, bg));
            table.addCell(pdf.numCell(pdf.formatAmount(ligne.getMontantTotal()), lineFont, bg));
        }

        doc.add(table);
    }

    /* ── Totals & payments ─────────────────────────────────────────────── */

    private void addTotalsAndPayments(Document doc, FactureClient facture) throws DocumentException {
        PdfPTable totals = new PdfPTable(new float[]{65, 35});
        totals.setWidthPercentage(100);

        Font labelFont = new Font(Font.HELVETICA, 9, Font.NORMAL, IPdfService.GRAY_TEXT);
        Font valueFont = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);
        Font boldFont  = new Font(Font.HELVETICA, 11, Font.BOLD, IPdfService.PRIMARY);

        pdf.addTotalRow(totals, pdf.msg("pdf.vente.totals.totalHt"), pdf.formatAmount(facture.getMontantTotal()), labelFont, valueFont, Color.WHITE);

        var paiements = paiementVenteDomainService.findAllByFactureId(facture.getId());
        for (var p : paiements) {
            String label = pdf.msg("pdf.vente.totals.paiement") + " (" + (p.getMoyen() != null ? p.getMoyen().getLibelle() : "—") + ")";
            if (p.getDatePaiement() != null) label += " – " + p.getDatePaiement().format(IPdfService.DATE_FMT);
            pdf.addTotalRow(totals, label, pdf.formatAmount(p.getMontant()), labelFont, valueFont, Color.WHITE);
        }

        BigDecimal reste = facture.getMontantTotal().subtract(facture.getMontantPaye());
        Color resteBg = reste.compareTo(BigDecimal.ZERO) == 0
                ? new Color(236, 253, 245) : new Color(255, 251, 235);
        pdf.addTotalRow(totals, pdf.msg("pdf.vente.totals.soldeRestant"), pdf.formatAmount(reste), boldFont, boldFont, resteBg);

        doc.add(totals);
    }
}
