package org.store.abonnement.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.abonnement.application.dto.RevenuPeriodFilter;
import org.store.abonnement.application.dto.RevenuRecordCommand;
import org.store.abonnement.application.service.IAbonnementService;
import org.store.abonnement.application.service.IRevenuService;
import org.store.abonnement.domain.model.Revenu;
import org.store.abonnement.domain.service.RevenuDomainService;
import org.store.country.domain.service.CountryDomainService;
import org.store.entreprise.application.service.IEntrepriseService;

import java.math.BigDecimal;
import java.util.UUID;

/** Persists validated-payment revenue rows and aggregates them for the platform P&L reporting endpoint. */
@Service
@Transactional(readOnly = true)
public class RevenuServiceImpl implements IRevenuService {

    private final RevenuDomainService revenuDomainService;
    private final IEntrepriseService entrepriseService;
    private final CountryDomainService countryDomainService;
    private final IAbonnementService abonnementService;

    public RevenuServiceImpl(RevenuDomainService revenuDomainService,
                             IEntrepriseService entrepriseService,
                             CountryDomainService countryDomainService,
                             IAbonnementService abonnementService) {
        this.revenuDomainService = revenuDomainService;
        this.entrepriseService = entrepriseService;
        this.countryDomainService = countryDomainService;
        this.abonnementService = abonnementService;
    }

    /** Builds the Revenu row from ids only — both FKs resolved via cheap, fresh PK lookups (not stale proxies). */
    @Override
    @Transactional
    public void record(RevenuRecordCommand command) {
        Revenu revenu = new Revenu();
        revenu.setEntreprise(entrepriseService.findById(command.entrepriseId()));
        revenu.setCountry(countryDomainService.findById(command.countryId()));
        revenu.setDatePaiement(command.datePaiement());
        revenu.setMontant(command.montant());
        revenuDomainService.save(revenu);
    }

    /** When abonnementId is set, resolves it to the owning entreprise (Abonnement is 1:1 per entreprise) before querying. */
    @Override
    public BigDecimal getTotalForPeriod(RevenuPeriodFilter filter) {
        UUID entrepriseId = filter.abonnementId() != null
                ? abonnementService.findById(filter.abonnementId()).getEntreprise().getId()
                : null;
        return revenuDomainService.sumByPeriod(filter, entrepriseId);
    }
}
