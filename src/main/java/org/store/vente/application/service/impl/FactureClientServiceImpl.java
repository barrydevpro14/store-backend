package org.store.vente.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.achat.domain.enums.StatutFacture;
import org.store.common.dto.DataCountResponse;
import org.store.common.exceptions.EntityException;
import org.store.common.service.ValidatorService;
import org.store.magasin.application.service.IMagasinService;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;
import org.store.vente.application.dto.FactureClientFilter;
import org.store.vente.application.dto.FactureClientResponse;
import org.store.vente.application.service.IFactureClientService;
import org.store.vente.domain.model.FactureClient;
import org.store.vente.domain.service.FactureClientDomainService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Lectures listing et detail unitaire des factures client. Le scoping entreprise est applique
 * dans chaque query JPQL ; l'acces magasin du caller est valide en amont via IMagasinService.
 */
@Service
@Transactional(readOnly = true)
public class FactureClientServiceImpl implements IFactureClientService {

    private final FactureClientDomainService factureClientDomainService;
    private final IMagasinService magasinService;
    private final ICurrentUserService currentUserService;
    private final ValidatorService validatorService;

    public FactureClientServiceImpl(FactureClientDomainService factureClientDomainService,
                                    IMagasinService magasinService,
                                    ICurrentUserService currentUserService,
                                    ValidatorService validatorService) {
        this.factureClientDomainService = factureClientDomainService;
        this.magasinService = magasinService;
        this.currentUserService = currentUserService;
        this.validatorService = validatorService;
    }

    /** Listing paginé filtré : valide le filter, vérifie l'accès magasin du caller, délègue au domain. */
    @Override
    public Page<FactureClientResponse> findAllByCurrentEntreprise(FactureClientFilter filter) {
        validatorService.validate(filter);
        UserPrincipal currentUser = currentUserService.getCurrent();
        magasinService.ensureAccessibleByCurrentUser(magasinService.findById(filter.magasinId()));
        return factureClientDomainService.findResponsesByFilter(filter, currentUser.entrepriseId());
    }

    @Override
    public DataCountResponse countAllUnpaid(UUID magasingId) {
        long countByMagasinIdAndStatut = factureClientDomainService.countByMagasinIdAndStatut(magasingId, List.of(StatutFacture.NON_PAYEE, StatutFacture.PARTIELLEMENT_PAYEE));
        return new DataCountResponse(countByMagasinIdAndStatut);
    }

    /** GET by id : projection JPQL scopée par l'entreprise du caller, throw notFound si absent. */
    @Override
    public FactureClientResponse findResponseById(UUID id) {
        UserPrincipal currentUser = currentUserService.getCurrent();
        return factureClientDomainService.findResponseById(id, currentUser.entrepriseId())
                .orElseThrow(() -> new EntityException("factureClient.notFound", id));
    }

    @Override
    public List<FactureClient> findDueOnDates(List<LocalDate> dates, List<StatutFacture> statutFactures) {
        return factureClientDomainService.findDueOnDates(dates, statutFactures);
    }
}
