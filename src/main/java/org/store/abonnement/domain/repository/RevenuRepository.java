package org.store.abonnement.domain.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.abonnement.domain.model.Revenu;
import org.store.common.repository.BaseRepository;

import java.math.BigDecimal;
import java.util.UUID;

public interface RevenuRepository extends BaseRepository<Revenu> {

    @Query("""
            SELECT COALESCE(SUM(revenu.montant), 0)
            FROM Revenu revenu
            WHERE (:startDate IS NULL OR :startDate = '' OR revenu.datePaiement >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR revenu.datePaiement <= CAST(:endDate AS date))
              AND (:countryId IS NULL OR revenu.country.id = :countryId)
              AND (:entrepriseId IS NULL OR revenu.entreprise.id = :entrepriseId)
            """)
    BigDecimal sumByPeriod(@Param("startDate") String startDate,
                           @Param("endDate") String endDate,
                           @Param("countryId") UUID countryId,
                           @Param("entrepriseId") UUID entrepriseId);
}
