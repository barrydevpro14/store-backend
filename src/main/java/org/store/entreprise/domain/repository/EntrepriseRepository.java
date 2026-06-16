package org.store.entreprise.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.entreprise.application.dto.EntrepriseResponse;
import org.store.entreprise.domain.model.Entreprise;

public interface EntrepriseRepository extends BaseRepository<Entreprise> {

    @Query("SELECT COUNT(entreprise) FROM Entreprise entreprise WHERE entreprise.actif = :actif")
    long countByActif(@Param("actif") boolean actif);

    @Query(value = """
            SELECT new org.store.entreprise.application.dto.EntrepriseResponse(entreprise)
            FROM Entreprise entreprise
            WHERE (:sigle IS NULL OR :sigle = '' OR LOWER(entreprise.sigle) LIKE :siglePattern)
              AND (:raisonSociale IS NULL OR :raisonSociale = '' OR LOWER(entreprise.RaisonSociale) LIKE :raisonSocialePattern)
              AND (:ninea IS NULL OR :ninea = '' OR LOWER(entreprise.ninea) LIKE :nineaPattern)
              AND (:rccm IS NULL OR :rccm = '' OR LOWER(entreprise.rccm) LIKE :rccmPattern)
              AND (:actif IS NULL OR entreprise.actif = :actif)
              AND (:startDate IS NULL OR :startDate = '' OR FUNCTION('DATE', entreprise.createdAt) >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR FUNCTION('DATE', entreprise.createdAt) <= CAST(:endDate AS date))
            ORDER BY entreprise.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(entreprise)
            FROM Entreprise entreprise
            WHERE (:sigle IS NULL OR :sigle = '' OR LOWER(entreprise.sigle) LIKE :siglePattern)
              AND (:raisonSociale IS NULL OR :raisonSociale = '' OR LOWER(entreprise.RaisonSociale) LIKE :raisonSocialePattern)
              AND (:ninea IS NULL OR :ninea = '' OR LOWER(entreprise.ninea) LIKE :nineaPattern)
              AND (:rccm IS NULL OR :rccm = '' OR LOWER(entreprise.rccm) LIKE :rccmPattern)
              AND (:actif IS NULL OR entreprise.actif = :actif)
              AND (:startDate IS NULL OR :startDate = '' OR FUNCTION('DATE', entreprise.createdAt) >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR FUNCTION('DATE', entreprise.createdAt) <= CAST(:endDate AS date))
            """)
    Page<EntrepriseResponse> findResponsesByFilter(
            @Param("sigle") String sigle,
            @Param("siglePattern") String siglePattern,
            @Param("raisonSociale") String raisonSociale,
            @Param("raisonSocialePattern") String raisonSocialePattern,
            @Param("ninea") String ninea,
            @Param("nineaPattern") String nineaPattern,
            @Param("rccm") String rccm,
            @Param("rccmPattern") String rccmPattern,
            @Param("actif") Boolean actif,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            Pageable pageable);
}
