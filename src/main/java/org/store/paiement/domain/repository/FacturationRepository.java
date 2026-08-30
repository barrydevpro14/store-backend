package org.store.paiement.domain.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.paiement.domain.model.Facturation;

import java.util.UUID;

public interface FacturationRepository extends BaseRepository<Facturation> {

    @Query("""
            SELECT COUNT(facturation) > 0
            FROM Facturation facturation
            WHERE facturation.moyenPaiement.id = :moyenPaiementId
              AND ((:paysId IS NULL AND facturation.pays IS NULL) OR facturation.pays.id = :paysId)
              AND (:excludeId IS NULL OR facturation.id <> :excludeId)
            """)
    boolean existsByMoyenAndPays(@Param("moyenPaiementId") UUID moyenPaiementId,
                                  @Param("paysId") UUID paysId,
                                  @Param("excludeId") UUID excludeId);
}
