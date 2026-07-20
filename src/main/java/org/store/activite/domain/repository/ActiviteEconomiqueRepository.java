package org.store.activite.domain.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.activite.application.dto.ActiviteEconomiqueResponse;
import org.store.activite.application.dto.ActiviteEconomiqueSummaryResponse;
import org.store.activite.domain.model.ActiviteEconomique;
import org.store.common.repository.BaseRepository;

import java.util.List;
import java.util.UUID;

public interface ActiviteEconomiqueRepository extends BaseRepository<ActiviteEconomique> {

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM ActiviteEconomique a WHERE LOWER(a.libelle) = LOWER(:libelle) AND a.actif = true")
    boolean existsByLibelleIgnoreCaseAndActifTrue(@Param("libelle") String libelle);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM ActiviteEconomique a WHERE LOWER(a.libelle) = LOWER(:libelle) AND a.actif = true AND a.id <> :id")
    boolean existsByLibelleIgnoreCaseAndActifTrueAndIdNot(@Param("libelle") String libelle, @Param("id") UUID id);

    @Query("SELECT new org.store.activite.application.dto.ActiviteEconomiqueResponse(a.id, a.libelle, a.description, a.actif) FROM ActiviteEconomique a ORDER BY a.libelle ASC")
    List<ActiviteEconomiqueResponse> findAllResponsesOrderByLibelleAsc();

    @Query("SELECT new org.store.activite.application.dto.ActiviteEconomiqueSummaryResponse(a.id, a.libelle) FROM ActiviteEconomique a WHERE a.actif = true ORDER BY a.libelle ASC")
    List<ActiviteEconomiqueSummaryResponse> findAllActiveSummariesOrderByLibelleAsc();
}
