package org.store.achat.application.pdf.strategy;

import org.springframework.stereotype.Service;
import org.store.achat.application.pdf.renderer.StandardBonCommandePdfRenderer;
import org.store.achat.domain.model.CommandeAchat;
import org.store.magasin.domain.model.Magasin;
import org.store.pdf.domain.enums.PdfFormat;
import org.store.pdf.domain.model.PdfFormatConfig;

@Service
public class A4BonCommandePdfStrategy implements BonCommandePdfStrategy {

    private final StandardBonCommandePdfRenderer renderer;

    public A4BonCommandePdfStrategy(StandardBonCommandePdfRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public PdfFormat supports() {
        return PdfFormat.A4;
    }

    @Override
    public byte[] generate(CommandeAchat commande, Magasin magasin, PdfFormatConfig config) {
        return renderer.render(commande, magasin, config);
    }
}
