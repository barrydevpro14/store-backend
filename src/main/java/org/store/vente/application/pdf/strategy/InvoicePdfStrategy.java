package org.store.vente.application.pdf.strategy;

import org.store.magasin.domain.model.Magasin;
import org.store.pdf.domain.enums.PdfFormat;
import org.store.pdf.domain.model.PdfFormatConfig;
import org.store.vente.domain.model.CommandeVente;
import org.store.vente.domain.model.FactureClient;

public interface InvoicePdfStrategy {

    PdfFormat supports();

    byte[] generate(FactureClient facture, Magasin magasin, PdfFormatConfig config);

    byte[] generateDevis(CommandeVente commande, Magasin magasin, PdfFormatConfig config);
}
