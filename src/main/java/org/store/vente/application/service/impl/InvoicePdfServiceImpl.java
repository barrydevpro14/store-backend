package org.store.vente.application.service.impl;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Generates a PDF invoice for a client sale using OpenPDF.
 * Layout: store header / invoice metadata / lines table / totals / payments.
 */
@Service
@Transactional(readOnly = true)
public class InvoicePdfServiceImpl implements IInvoicePdfService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Color PRIMARY   = new Color(37, 99, 235);   // blue-600
    private static final Color LIGHT_BG  = new Color(239, 246, 255); // blue-50
    private static final Color GRAY_TEXT = new Color(107, 114, 128); // gray-500
    private static final Color BORDER    = new Color(229, 231, 235); // gray-200

    private final FactureClientDomainService factureClientDomainService;
    private final PaiementVenteDomainService paiementVenteDomainService;
    private final ICurrentUserService currentUserService;

    public InvoicePdfServiceImpl(FactureClientDomainService factureClientDomainService,
                                  PaiementVenteDomainService paiementVenteDomainService,
                                  ICurrentUserService currentUserService) {
        this.factureClientDomainService = factureClientDomainService;
        this.paiementVenteDomainService = paiementVenteDomainService;
        this.currentUserService = currentUserService;
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
            addFooter(doc, magasin);

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

        // Store info
        PdfPCell storeCell = new PdfPCell();
        storeCell.setBorder(Rectangle.NO_BORDER);
        storeCell.setPadding(8);

        Font nameFont = new Font(Font.HELVETICA, 16, Font.BOLD, PRIMARY);
        Font infoFont = new Font(Font.HELVETICA, 9, Font.NORMAL, GRAY_TEXT);

        storeCell.addElement(new Paragraph(magasin.getNom(), nameFont));
        if (isNotBlank(magasin.getAdresse()))
            storeCell.addElement(new Paragraph(magasin.getAdresse(), infoFont));
        if (isNotBlank(magasin.getTelephone()))
            storeCell.addElement(new Paragraph(magasin.getTelephone(), infoFont));

        header.addCell(storeCell);

        // Invoice title block
        PdfPCell invoiceCell = new PdfPCell();
        invoiceCell.setBorder(Rectangle.NO_BORDER);
        invoiceCell.setBackgroundColor(LIGHT_BG);
        invoiceCell.setPadding(12);
        invoiceCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        Font titleFont = new Font(Font.HELVETICA, 20, Font.BOLD, PRIMARY);
        Font numFont   = new Font(Font.HELVETICA, 11, Font.BOLD, Color.DARK_GRAY);
        Font dateFont  = new Font(Font.HELVETICA, 9, Font.NORMAL, GRAY_TEXT);

        invoiceCell.addElement(new Paragraph("FACTURE", titleFont));
        invoiceCell.addElement(new Paragraph(facture.getNumero(), numFont));
        if (facture.getDate() != null)
            invoiceCell.addElement(new Paragraph("Date : " + facture.getDate().format(DATE_FMT), dateFont));
        if (facture.getDateEcheance() != null)
            invoiceCell.addElement(new Paragraph("Échéance : " + facture.getDateEcheance().format(DATE_FMT), dateFont));

        header.addCell(invoiceCell);
        doc.add(header);
    }

    /* ── Client & meta ─────────────────────────────────────────────────── */

    private void addClientAndMeta(Document doc, FactureClient facture) throws DocumentException {
        Font valueFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.DARK_GRAY);

        PdfPCell clientCell = sectionCell("CLIENT");
        if (facture.getCommande().getClient() != null) {
            var client = facture.getCommande().getClient();
            clientCell.addElement(new Paragraph(client.getNom() + " " + nullToEmpty(client.getPrenom()), valueFont));
            if (isNotBlank(client.getTelephone()))
                clientCell.addElement(new Paragraph(client.getTelephone(), new Font(Font.HELVETICA, 9, Font.NORMAL, GRAY_TEXT)));
            if (isNotBlank(client.getEmail()))
                clientCell.addElement(new Paragraph(client.getEmail(), new Font(Font.HELVETICA, 9, Font.NORMAL, GRAY_TEXT)));
        } else {
            clientCell.addElement(new Paragraph("Client anonyme", new Font(Font.HELVETICA, 9, Font.ITALIC, GRAY_TEXT)));
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
        String[] headers = {"Produit", "Qté", "Prix unitaire", "Total HT"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headFont));
            cell.setBackgroundColor(PRIMARY);
            cell.setPadding(7);
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setHorizontalAlignment(h.equals("Produit") ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT);
            table.addCell(cell);
        }

        Font lineFont  = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);
        Font refFont   = new Font(Font.HELVETICA, 8, Font.NORMAL, GRAY_TEXT);
        boolean alt    = false;

        for (LigneCommandeVente ligne : facture.getCommande().getLignes()) {
            Color bg = alt ? new Color(249, 250, 251) : Color.WHITE;
            alt = !alt;

            PdfPCell nameCell = new PdfPCell();
            nameCell.setBackgroundColor(bg);
            nameCell.setBorderColor(BORDER);
            nameCell.setPadding(6);
            String nom = ligne.getProductFournisseur().getProduct().getNom();
            String ref = ligne.getProductFournisseur().getProduct().getReference();
            nameCell.addElement(new Paragraph(nom, lineFont));
            if (isNotBlank(ref))
                nameCell.addElement(new Paragraph("Réf: " + ref, refFont));
            table.addCell(nameCell);

            table.addCell(numCell(String.valueOf(ligne.getQuantite()), lineFont, bg));
            table.addCell(numCell(formatAmount(ligne.getPrixUnitaire()), lineFont, bg));
            table.addCell(numCell(formatAmount(ligne.getMontantTotal()), lineFont, bg));
        }

        doc.add(table);
    }

    /* ── Totals & payments ─────────────────────────────────────────────── */

    private void addTotalsAndPayments(Document doc, FactureClient facture) throws DocumentException {
        PdfPTable totals = new PdfPTable(new float[]{65, 35});
        totals.setWidthPercentage(100);

        Font labelFont = new Font(Font.HELVETICA, 9, Font.NORMAL, GRAY_TEXT);
        Font valueFont = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);
        Font boldFont  = new Font(Font.HELVETICA, 11, Font.BOLD, PRIMARY);

        addTotalRow(totals, "Total HT", formatAmount(facture.getMontantTotal()), labelFont, valueFont, Color.WHITE);

        var paiements = paiementVenteDomainService.findAllByFactureId(facture.getId());
        for (var p : paiements) {
            String label = "Paiement (" + (p.getMoyen() != null ? p.getMoyen().getLibelle() : "—") + ")";
            if (p.getDatePaiement() != null) label += " – " + p.getDatePaiement().format(DATE_FMT);
            addTotalRow(totals, label, formatAmount(p.getMontant()), labelFont, valueFont, Color.WHITE);
        }

        BigDecimal reste = facture.getMontantTotal().subtract(facture.getMontantPaye());
        Color resteBg = reste.compareTo(BigDecimal.ZERO) == 0
                ? new Color(236, 253, 245) : new Color(255, 251, 235);
        addTotalRow(totals, "Solde restant", formatAmount(reste), boldFont, boldFont, resteBg);

        doc.add(totals);
    }

    private void addTotalRow(PdfPTable table, String label, String value,
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

    /* ── Footer ────────────────────────────────────────────────────────── */

    private void addFooter(Document doc, Magasin magasin) throws DocumentException {
        doc.add(Chunk.NEWLINE);
        Font footerFont = new Font(Font.HELVETICA, 8, Font.ITALIC, GRAY_TEXT);
        String text = "Document généré par Store ERP";
        if (isNotBlank(magasin.getNom())) text = magasin.getNom() + " – " + text;
        Paragraph footer = new Paragraph(text, footerFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer);
    }

    /* ── Helpers ───────────────────────────────────────────────────────── */

    private PdfPCell sectionCell(String title) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(BORDER);
        cell.setPadding(10);
        cell.addElement(new Paragraph(title, new Font(Font.HELVETICA, 8, Font.BOLD, GRAY_TEXT)));
        cell.addElement(Chunk.NEWLINE);
        return cell;
    }

    private PdfPCell numCell(String text, Font font, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setBorderColor(BORDER);
        cell.setBackgroundColor(bg);
        cell.setPadding(6);
        return cell;
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "0";
        return String.format("%,.0f", amount);
    }

    private boolean isNotBlank(String s) { return s != null && !s.isBlank(); }
    private String nullToEmpty(String s)  { return s == null ? "" : s; }
}
