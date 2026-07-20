package org.store.activite.domain.service;

import org.springframework.stereotype.Service;
import org.store.activite.application.dto.ActiviteEconomiqueResponse;
import org.store.activite.application.dto.ActiviteEconomiqueSummaryResponse;
import org.store.activite.domain.model.ActiviteEconomique;
import org.store.activite.domain.repository.ActiviteEconomiqueRepository;
import org.store.common.service.GlobalService;

import java.util.List;
import java.util.UUID;

@Service
public class ActiviteEconomiqueDomainService extends GlobalService<ActiviteEconomique, ActiviteEconomiqueRepository> {

    public ActiviteEconomiqueDomainService(ActiviteEconomiqueRepository repository) {
        super(repository);
    }

    public boolean existsByLibelleIgnoreCaseAndActifTrue(String libelle) {
        return repository.existsByLibelleIgnoreCaseAndActifTrue(libelle);
    }

    public boolean existsByLibelleIgnoreCaseAndActifTrueAndIdNot(String libelle, UUID id) {
        return repository.existsByLibelleIgnoreCaseAndActifTrueAndIdNot(libelle, id);
    }

    public void activate(ActiviteEconomique activite) {
        activite.setActif(true);
        save(activite);
    }

    public void deactivate(ActiviteEconomique activite) {
        activite.setActif(false);
        save(activite);
    }

    public List<ActiviteEconomiqueResponse> findAllOrderByLibelleAsc() {
        return repository.findAllResponsesOrderByLibelleAsc();
    }

    public List<ActiviteEconomiqueSummaryResponse> findAllActiveOrderByLibelleAsc() {
        return repository.findAllActiveSummariesOrderByLibelleAsc();
    }
}
