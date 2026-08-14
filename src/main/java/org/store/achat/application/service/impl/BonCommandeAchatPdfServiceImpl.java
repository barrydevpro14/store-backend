package org.store.achat.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.achat.application.pdf.strategy.BonCommandePdfStrategy;
import org.store.achat.application.pdf.strategy.BonCommandePdfStrategyResolver;
import org.store.achat.application.service.IBonCommandeAchatPdfService;
import org.store.achat.application.service.ICommandeAchatService;
import org.store.achat.domain.model.CommandeAchat;
import org.store.common.tools.OwnershipHelper;
import org.store.pdf.application.service.IPdfFormatConfigService;
import org.store.pdf.domain.model.PdfFormatConfig;
import org.store.security.application.service.ICurrentUserService;

import java.util.UUID;

/**
 * Orchestre la génération du PDF bon de commande achat :
 * résolution de la config format, ownership check, puis dispatch vers la strategy.
 */
@Service
@Transactional(readOnly = true)
public class BonCommandeAchatPdfServiceImpl implements IBonCommandeAchatPdfService {

    private final ICommandeAchatService commandeAchatService;
    private final IPdfFormatConfigService pdfFormatConfigService;
    private final BonCommandePdfStrategyResolver strategyResolver;
    private final ICurrentUserService currentUserService;

    public BonCommandeAchatPdfServiceImpl(ICommandeAchatService commandeAchatService,
                                           IPdfFormatConfigService pdfFormatConfigService,
                                           BonCommandePdfStrategyResolver strategyResolver,
                                           ICurrentUserService currentUserService) {
        this.commandeAchatService = commandeAchatService;
        this.pdfFormatConfigService = pdfFormatConfigService;
        this.strategyResolver = strategyResolver;
        this.currentUserService = currentUserService;
    }

    /** Génère les bytes PDF du bon de commande dans le format identifié par configId. */
    @Override
    public byte[] generate(UUID commandeId, UUID configId) {
        CommandeAchat commande = commandeAchatService.findById(commandeId);

        OwnershipHelper.ensureOwnership(
                commande,
                commande.getMagasin().getEntreprise().getId(),
                currentUserService.getCurrent().entrepriseId(),
                "commandeAchat.notOwned"
        );

        PdfFormatConfig config = pdfFormatConfigService.findById(configId);
        BonCommandePdfStrategy strategy = strategyResolver.resolve(config.getFormat());

        return strategy.generate(commande, commande.getMagasin(), config);
    }
}
