package org.store.achat.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.achat.application.dto.FactureAchatEcheanceFilter;
import org.store.achat.application.dto.FactureAchatFilter;
import org.store.achat.application.dto.FactureAchatResponse;
import org.store.achat.application.service.IFactureAchatService;
import org.store.achat.domain.enums.StatutFacture;
import org.store.achat.domain.model.FactureAchat;
import org.store.achat.domain.service.FactureAchatDomainService;
import org.store.common.exceptions.EntityException;
import org.store.common.service.ValidatorService;
import org.store.common.tools.OwnershipHelper;
import org.store.security.application.service.ICurrentUserService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Lecture des factures d'achat, scopée sur l'entreprise du caller.
 */
@Service
@Transactional(readOnly = true)
public class FactureAchatServiceImpl implements IFactureAchatService {

    private final FactureAchatDomainService factureAchatDomainService;
    private final ICurrentUserService currentUserService;
    private final ValidatorService validatorService;

    public FactureAchatServiceImpl(FactureAchatDomainService factureAchatDomainService,
                                   ICurrentUserService currentUserService,
                                   ValidatorService validatorService) {
        this.factureAchatDomainService = factureAchatDomainService;
        this.currentUserService = currentUserService;
        this.validatorService = validatorService;
    }

    /** Retourne la facture après vérification d'appartenance à l'entreprise du caller. */
    @Override
    public FactureAchatResponse findResponseById(UUID id) {
        FactureAchat facture = factureAchatDomainService.findById(id);
        OwnershipHelper.ensureOwnership(
                facture,
                facture.getCommande().getMagasin().getEntreprise().getId(),
                currentUserService.getCurrent().entrepriseId(),
                "factureAchat.notOwned"
        );
        return new FactureAchatResponse(facture);
    }

    /** Valide le filter et délègue la query scopée par entreprise. */
    @Override
    public Page<FactureAchatResponse> findAllByCurrentEntreprise(FactureAchatFilter factureAchatFilter) {
        validatorService.validate(factureAchatFilter);
        return factureAchatDomainService.findResponsesByFilter(factureAchatFilter, currentUserService.getCurrent().entrepriseId());
    }

    /** Valide le filter et délègue la query d'échéances scopée par entreprise. */
    @Override
    public Page<FactureAchatResponse> findEcheances(FactureAchatEcheanceFilter factureAchatEcheanceFilter) {
        validatorService.validate(factureAchatEcheanceFilter);
        return factureAchatDomainService.findEcheances(factureAchatEcheanceFilter, currentUserService.getCurrent().entrepriseId());
    }

    /** Retourne l'entité facture liée à une commande (cardinalité 1-1) ou lève `EntityException`. */
    @Override
    public FactureAchat findByCommandeId(UUID commandeId) {
        return factureAchatDomainService.findByCommandeId(commandeId)
                .orElseThrow(() -> new EntityException("factureAchat.notFoundForCommande", commandeId));
    }

    /**
     * @param dates 
     * @param statutFactures
     * @return
     */
    @Override
    public List<FactureAchat> findDueOnDates(List<LocalDate> dates, List<StatutFacture> statutFactures) {
        return factureAchatDomainService.findDueOnDates(dates, statutFactures);
    }
}
