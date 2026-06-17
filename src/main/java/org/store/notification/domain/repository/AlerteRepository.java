package org.store.notification.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.notification.domain.enums.AlerteStatut;
import org.store.notification.domain.enums.AlerteType;
import org.store.notification.domain.model.Alerte;

import java.util.List;
import java.util.UUID;

public interface AlerteRepository extends BaseRepository<Alerte> {

    @Query(value = """
            SELECT * FROM alerte a
            WHERE (:entrepriseIdStr = '' OR a.entreprise_id::text = :entrepriseIdStr)
              AND (:magasinIdStr    = '' OR a.magasin_id::text    = :magasinIdStr)
              AND (:typeStr         = '' OR a.type                = :typeStr)
              AND (:statutStr       = '' OR a.statut              = :statutStr)
              AND (:fromStr         = '' OR a.created_at          >= CAST(:fromStr AS date))
              AND (:toStr           = '' OR a.created_at          <= CAST(:toStr AS date) + INTERVAL '1 day')
            ORDER BY a.created_at DESC
            """,
           countQuery = """
            SELECT COUNT(*) FROM alerte a
            WHERE (:entrepriseIdStr = '' OR a.entreprise_id::text = :entrepriseIdStr)
              AND (:magasinIdStr    = '' OR a.magasin_id::text    = :magasinIdStr)
              AND (:typeStr         = '' OR a.type                = :typeStr)
              AND (:statutStr       = '' OR a.statut              = :statutStr)
              AND (:fromStr         = '' OR a.created_at          >= CAST(:fromStr AS date))
              AND (:toStr           = '' OR a.created_at          <= CAST(:toStr AS date) + INTERVAL '1 day')
            """,
           nativeQuery = true)
    Page<Alerte> findByFilterNative(@Param("entrepriseIdStr") String entrepriseIdStr,
                                    @Param("magasinIdStr")    String magasinIdStr,
                                    @Param("typeStr")         String typeStr,
                                    @Param("statutStr")       String statutStr,
                                    @Param("fromStr")         String fromStr,
                                    @Param("toStr")           String toStr,
                                    Pageable pageable);

    @Query("""
    SELECT COUNT(a) FROM Alerte a WHERE a.entrepriseId = :entrepriseId
    AND a.magasinId = :magasinId AND a.type IN :alerteTypes
    AND a.statut = :statut
""")
    Long countNouvelles(UUID entrepriseId, UUID magasinId, List<AlerteType> alerteTypes , AlerteStatut statut);
}
