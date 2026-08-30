package org.store.paiement.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.service.GlobalService;
import org.store.common.tools.DateHelper;
import org.store.common.tools.LikePatternHelper;
import org.store.country.domain.model.Country;
import org.store.paiement.application.dto.FacturationFilter;
import org.store.paiement.application.dto.FacturationOptionResponse;
import org.store.paiement.application.dto.FacturationRequest;
import org.store.paiement.application.dto.FacturationResponse;
import org.store.paiement.domain.model.Facturation;
import org.store.paiement.domain.model.MoyenPaiement;
import org.store.paiement.domain.repository.FacturationRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class FacturationDomainService extends GlobalService<Facturation, FacturationRepository> {

    public FacturationDomainService(FacturationRepository repository) {
        super(repository);
    }

    public Facturation create(FacturationRequest request, MoyenPaiement moyenPaiement, Set<Country> pays) {
        Facturation facturation = new Facturation();
        facturation.setMoyenPaiement(moyenPaiement);
        facturation.setPays(pays);
        facturation.setNumeroFacturation(request.numeroFacturation());
        facturation.setActif(true);
        return save(facturation);
    }

    /**
     * Enforces that, for a given moyen, no two facturations share a country and at most one is
     * global (empty pays) — a country-specific facturation is still allowed to coexist with a
     * global one for the same moyen, since {@code findSelectOptions} always prefers the
     * country-specific match.
     */
    public void ensureNoCountryOverlap(UUID moyenPaiementId, Set<UUID> paysIds, UUID excludeId) {
        if (paysIds == null || paysIds.isEmpty()) {
            if (repository.existsGlobal(moyenPaiementId, excludeId)) {
                throw new BadArgumentException("facturation.alreadyExists");
            }
            return;
        }
        if (repository.existsWithOverlappingCountry(moyenPaiementId, paysIds, excludeId)) {
            throw new BadArgumentException("facturation.alreadyExists");
        }
    }

    public Page<FacturationResponse> findResponsesByFilter(FacturationFilter filter) {
        String numeroFacturationPattern = LikePatternHelper.toLikePattern(filter.numeroFacturation());
        return repository.findResponsesByFilter(
                filter.moyenPaiementId(), filter.paysId(), numeroFacturationPattern, filter.actif(),
                DateHelper.coalesceStart(filter.createdStartDateTime()), DateHelper.coalesceEnd(filter.createdEndDateTime()),
                filter.toPageable());
    }

    /** Country-specific matches first, then global facturations not overridden for that country. */
    public List<FacturationOptionResponse> findSelectOptions(UUID countryId) {
        List<FacturationOptionResponse> options = new ArrayList<>(repository.findCountrySpecificOptions(countryId));
        options.addAll(repository.findGlobalOptionsWithoutOverrideFor(countryId));
        options.sort(Comparator.comparing(FacturationOptionResponse::moyenLibelle));
        return options;
    }
}
