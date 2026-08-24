package org.store.abonnement.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.abonnement.application.dto.RevenuPeriodFilter;
import org.store.abonnement.application.dto.RevenuRecordCommand;
import org.store.abonnement.application.service.IRevenuService;
import org.store.abonnement.domain.model.Revenu;
import org.store.abonnement.domain.service.RevenuDomainService;

import java.math.BigDecimal;

/** Persists validated-payment revenue rows and aggregates them for the platform P&L reporting endpoint. */
@Service
@Transactional(readOnly = true)
public class RevenuServiceImpl implements IRevenuService {

    private final RevenuDomainService revenuDomainService;

    public RevenuServiceImpl(RevenuDomainService revenuDomainService) {
        this.revenuDomainService = revenuDomainService;
    }

    /** Builds the Revenu row directly from the already-loaded Entreprise — called synchronously from validate(), same transaction. */
    @Override
    @Transactional
    public void record(RevenuRecordCommand command) {
        Revenu revenu = new Revenu();
        revenu.setEntreprise(command.entreprise());
        revenu.setCountry(command.entreprise().getCountry());
        revenu.setDatePaiement(command.datePaiement());
        revenu.setMontant(command.montant());
        revenuDomainService.save(revenu);
    }

    @Override
    public BigDecimal getTotalForPeriod(RevenuPeriodFilter filter) {
        return revenuDomainService.sumByPeriod(filter);
    }
}
