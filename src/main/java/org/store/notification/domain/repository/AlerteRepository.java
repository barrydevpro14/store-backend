package org.store.notification.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.notification.domain.model.Alerte;

import java.time.LocalDateTime;

public interface AlerteRepository extends BaseRepository<Alerte> {

    /**
     * Native query with sentinel-based optional filtering to avoid PostgreSQL type-inference issues
     * with nullable JDBC parameters.
     * Pass '' for typeStr/statutStr/entrepriseIdStr/magasinIdStr to mean "no filter".
     * Pass SENTINEL_START / SENTINEL_END for from/to to mean "no date filter".
     */
    @Query(value = """
            SELECT * FROM alerte a
            WHERE (:entrepriseIdStr = '' OR a.entreprise_id::text = :entrepriseIdStr)
              AND (:magasinIdStr    = '' OR a.magasin_id::text    = :magasinIdStr)
              AND (:typeStr         = '' OR a.type                = :typeStr)
              AND (:statutStr       = '' OR a.statut              = :statutStr)
              AND a.created_at >= :from
              AND a.created_at <= :to
            ORDER BY a.created_at DESC
            """,
           countQuery = """
            SELECT COUNT(*) FROM alerte a
            WHERE (:entrepriseIdStr = '' OR a.entreprise_id::text = :entrepriseIdStr)
              AND (:magasinIdStr    = '' OR a.magasin_id::text    = :magasinIdStr)
              AND (:typeStr         = '' OR a.type                = :typeStr)
              AND (:statutStr       = '' OR a.statut              = :statutStr)
              AND a.created_at >= :from
              AND a.created_at <= :to
            """,
           nativeQuery = true)
    Page<Alerte> findByFilterNative(@Param("entrepriseIdStr") String entrepriseIdStr,
                                    @Param("magasinIdStr")    String magasinIdStr,
                                    @Param("typeStr")         String typeStr,
                                    @Param("statutStr")       String statutStr,
                                    @Param("from")            LocalDateTime from,
                                    @Param("to")              LocalDateTime to,
                                    Pageable pageable);
}
