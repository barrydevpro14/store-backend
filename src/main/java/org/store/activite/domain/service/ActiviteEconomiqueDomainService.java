package org.store.activite.domain.service;

import org.springframework.stereotype.Service;
import org.store.activite.application.dto.ActiviteEconomiqueSummaryResponse;
import org.store.activite.domain.model.ActiviteEconomique;
import org.store.activite.domain.repository.ActiviteEconomiqueRepository;
import org.store.common.service.GlobalService;

import java.util.List;
import java.util.Optional;

@Service
public class ActiviteEconomiqueDomainService extends GlobalService<ActiviteEconomique, ActiviteEconomiqueRepository> {

    public ActiviteEconomiqueDomainService(ActiviteEconomiqueRepository repository) {
        super(repository);
    }

    public boolean existsByLibelle(String libelle) {
        return repository.existsByLibelle(libelle);
    }

    public Optional<ActiviteEconomique> findByLibelle(String libelle) {
        return repository.findByLibelle(libelle);
    }

    public List<ActiviteEconomiqueSummaryResponse> findAllOrderByLibelleAsc() {
        return repository.findAllSummariesOrderByLibelleAsc();
    }
}
