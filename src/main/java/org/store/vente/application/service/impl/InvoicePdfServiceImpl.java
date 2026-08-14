package org.store.vente.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.tools.OwnershipHelper;
import org.store.pdf.application.service.IPdfFormatConfigService;
import org.store.pdf.domain.model.PdfFormatConfig;
import org.store.security.application.service.ICurrentUserService;
import org.store.vente.application.service.IFactureClientService;
import org.store.vente.application.service.IInvoicePdfService;
import org.store.vente.application.pdf.strategy.InvoicePdfStrategy;
import org.store.vente.application.pdf.strategy.InvoicePdfStrategyResolver;
import org.store.vente.domain.model.FactureClient;

import java.util.UUID;

/**
 * Orchestre la génération du PDF de facture client :
 * résolution de la config format, ownership check, puis dispatch vers la strategy.
 */
@Service
@Transactional(readOnly = true)
public class InvoicePdfServiceImpl implements IInvoicePdfService {

    private final IFactureClientService factureClientService;
    private final IPdfFormatConfigService pdfFormatConfigService;
    private final InvoicePdfStrategyResolver strategyResolver;
    private final ICurrentUserService currentUserService;

    public InvoicePdfServiceImpl(IFactureClientService factureClientService,
                                  IPdfFormatConfigService pdfFormatConfigService,
                                  InvoicePdfStrategyResolver strategyResolver,
                                  ICurrentUserService currentUserService) {
        this.factureClientService = factureClientService;
        this.pdfFormatConfigService = pdfFormatConfigService;
        this.strategyResolver = strategyResolver;
        this.currentUserService = currentUserService;
    }

    /** Génère les bytes PDF de la facture dans le format identifié par configId. */
    @Override
    public byte[] generate(UUID factureId, UUID configId) {
        FactureClient facture = factureClientService.findById(factureId);

        OwnershipHelper.ensureOwnership(
                facture,
                facture.getCommande().getMagasin().getEntreprise().getId(),
                currentUserService.getCurrent().entrepriseId(),
                "factureClient.notOwned"
        );

        PdfFormatConfig config = pdfFormatConfigService.findById(configId);
        InvoicePdfStrategy strategy = strategyResolver.resolve(config.getFormat());

        return strategy.generate(facture, facture.getCommande().getMagasin(), config);
    }
}
