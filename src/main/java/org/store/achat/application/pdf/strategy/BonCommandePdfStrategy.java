package org.store.achat.application.pdf.strategy;

import org.store.achat.domain.model.CommandeAchat;
import org.store.magasin.domain.model.Magasin;
import org.store.pdf.domain.enums.PdfFormat;
import org.store.pdf.domain.model.PdfFormatConfig;

public interface BonCommandePdfStrategy {

    PdfFormat supports();

    byte[] generate(CommandeAchat commande, Magasin magasin, PdfFormatConfig config);
}
