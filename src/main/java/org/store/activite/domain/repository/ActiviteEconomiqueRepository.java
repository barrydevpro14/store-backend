package org.store.activite.domain.repository;

import org.springframework.data.jpa.repository.Query;
import org.store.activite.application.dto.ActiviteEconomiqueSummaryResponse;
import org.store.activite.domain.model.ActiviteEconomique;
import org.store.common.repository.BaseRepository;

import java.util.List;
import java.util.Optional;

public interface ActiviteEconomiqueRepository extends BaseRepository<ActiviteEconomique> {

    boolean existsByLibelle(String libelle);

    Optional<ActiviteEconomique> findByLibelle(String libelle);

    @Query("SELECT new org.store.activite.application.dto.ActiviteEconomiqueSummaryResponse(a.id, a.libelle) FROM ActiviteEconomique a ORDER BY a.libelle ASC")
    List<ActiviteEconomiqueSummaryResponse> findAllSummariesOrderByLibelleAsc();
}
