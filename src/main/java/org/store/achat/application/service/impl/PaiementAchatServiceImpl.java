package org.store.achat.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.achat.application.dto.PaiementAchatCreate;
import org.store.achat.application.dto.PaiementAchatRequest;
import org.store.achat.application.dto.PaiementAchatResponse;
import org.store.achat.application.service.IPaiementAchatService;
import org.store.achat.domain.model.FactureAchat;
import org.store.achat.domain.model.PaiementAchat;
import org.store.achat.domain.service.FactureAchatDomainService;
import org.store.achat.domain.service.PaiementAchatDomainService;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.service.ValidatorService;
import org.store.common.tools.OwnershipHelper;
import org.store.security.application.service.ICurrentUserService;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Orchestre l'enregistrement d'un paiement fournisseur et la mise à jour de la facture associée.
 */
@Service
@Transactional(readOnly = true)
public class PaiementAchatServiceImpl implements IPaiementAchatService {

    private final FactureAchatDomainService factureAchatDomainService;
    private final PaiementAchatDomainService paiementAchatDomainService;
    private final ICurrentUserService currentUserService;
    private final ValidatorService validatorService;

    public PaiementAchatServiceImpl(FactureAchatDomainService factureAchatDomainService,
                                    PaiementAchatDomainService paiementAchatDomainService,
                                    ICurrentUserService currentUserService,
                                    ValidatorService validatorService) {
        this.factureAchatDomainService = factureAchatDomainService;
        this.paiementAchatDomainService = paiementAchatDomainService;
        this.currentUserService = currentUserService;
        this.validatorService = validatorService;
    }

    /** Vérifie l'accès facture, refuse l'overpaiement, crée le paiement et met à jour la facture. */
    @Override
    @Transactional
    public PaiementAchatResponse create(UUID factureId, PaiementAchatRequest paiementAchatRequest) {
        validatorService.validate(paiementAchatRequest);

        FactureAchat facture = factureAchatDomainService.findById(factureId);
        ensureFactureBelongsToCurrentEntreprise(facture);

        BigDecimal montantPaye = facture.getMontantPaye() != null ? facture.getMontantPaye() : BigDecimal.ZERO;
        BigDecimal restant = facture.getMontantTotal().subtract(montantPaye);
        if (paiementAchatRequest.montant().compareTo(restant) > 0) {
            throw new BadArgumentException("paiementAchat.montant.exceedsRemaining", restant);
        }

        PaiementAchat paiement = paiementAchatDomainService.create(new PaiementAchatCreate(
                facture, paiementAchatRequest.montant(),
                paiementAchatRequest.datePaiement(), paiementAchatRequest.moyen()
        ));

        factureAchatDomainService.applyPaiement(facture, paiementAchatRequest.montant());

        return new PaiementAchatResponse(paiement);
    }

    /** Liste paginée scopée par entreprise après check d'accès facture. */
    @Override
    public Page<PaiementAchatResponse> findByFactureId(UUID factureId, Pageable pageable) {
        FactureAchat facture = factureAchatDomainService.findById(factureId);
        ensureFactureBelongsToCurrentEntreprise(facture);
        return paiementAchatDomainService.findResponsesByFactureId(factureId, pageable);
    }

    /** Lève ForbiddenException si la facture n'appartient pas à l'entreprise du caller (via commande.magasin.entreprise). */
    public void ensureFactureBelongsToCurrentEntreprise(FactureAchat facture) {
        OwnershipHelper.ensureOwnership(
                facture,
                facture.getCommande().getMagasin().getEntreprise().getId(),
                currentUserService.getCurrent().entrepriseId(),
                "factureAchat.notOwned"
        );
    }
}
