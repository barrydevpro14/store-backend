package org.store.entreprise.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.entreprise.application.dto.EntrepriseFilter;
import org.store.entreprise.application.dto.EntrepriseResponse;
import org.store.entreprise.domain.model.Entreprise;

public interface EntrepriseRepository extends BaseRepository<Entreprise> {

    @Query(value = """
            SELECT new org.store.entreprise.application.dto.EntrepriseResponse(entreprise)
            FROM Entreprise entreprise
            WHERE (:#{#filter.sigle} IS NULL OR LOWER(entreprise.sigle) LIKE LOWER(CONCAT('%', :#{#filter.sigle}, '%')))
              AND (:#{#filter.raisonSociale} IS NULL OR LOWER(entreprise.RaisonSociale) LIKE LOWER(CONCAT('%', :#{#filter.raisonSociale}, '%')))
              AND (:#{#filter.ninea} IS NULL OR LOWER(entreprise.ninea) LIKE LOWER(CONCAT('%', :#{#filter.ninea}, '%')))
              AND (:#{#filter.rccm} IS NULL OR LOWER(entreprise.rccm) LIKE LOWER(CONCAT('%', :#{#filter.rccm}, '%')))
              AND (:#{#filter.actif} IS NULL OR entreprise.actif = :#{#filter.actif})
              AND (:#{#filter.createdStartDateTime()} IS NULL OR entreprise.createdAt >= :#{#filter.createdStartDateTime()})
              AND (:#{#filter.createdEndDateTime()}   IS NULL OR entreprise.createdAt <  :#{#filter.createdEndDateTime()})
            ORDER BY entreprise.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(entreprise)
            FROM Entreprise entreprise
            WHERE (:#{#filter.sigle} IS NULL OR LOWER(entreprise.sigle) LIKE LOWER(CONCAT('%', :#{#filter.sigle}, '%')))
              AND (:#{#filter.raisonSociale} IS NULL OR LOWER(entreprise.RaisonSociale) LIKE LOWER(CONCAT('%', :#{#filter.raisonSociale}, '%')))
              AND (:#{#filter.ninea} IS NULL OR LOWER(entreprise.ninea) LIKE LOWER(CONCAT('%', :#{#filter.ninea}, '%')))
              AND (:#{#filter.rccm} IS NULL OR LOWER(entreprise.rccm) LIKE LOWER(CONCAT('%', :#{#filter.rccm}, '%')))
              AND (:#{#filter.actif} IS NULL OR entreprise.actif = :#{#filter.actif})
              AND (:#{#filter.createdStartDateTime()} IS NULL OR entreprise.createdAt >= :#{#filter.createdStartDateTime()})
              AND (:#{#filter.createdEndDateTime()}   IS NULL OR entreprise.createdAt <  :#{#filter.createdEndDateTime()})
            """)
    Page<EntrepriseResponse> findResponsesByFilter(@Param("filter") EntrepriseFilter filter, Pageable pageable);
}
