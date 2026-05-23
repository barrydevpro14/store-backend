package org.store.magasin.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.magasin.application.dto.MagasinFilter;
import org.store.magasin.application.dto.MagasinResponse;
import org.store.magasin.domain.model.Magasin;

import java.util.UUID;

public interface MagasinRepository extends BaseRepository<Magasin> {

    @Query(value = """
            SELECT new org.store.magasin.application.dto.MagasinResponse(magasin)
            FROM Magasin magasin
            WHERE magasin.entreprise.id = :entrepriseId
              AND (:#{#filter.nom} IS NULL OR LOWER(magasin.nom) LIKE LOWER(CONCAT('%', :#{#filter.nom}, '%')))
              AND (:#{#filter.actif} IS NULL OR magasin.actif = :#{#filter.actif})
              AND (:#{#filter.createdStartDateTime()} IS NULL OR magasin.createdAt >= :#{#filter.createdStartDateTime()})
              AND (:#{#filter.createdEndDateTime()}   IS NULL OR magasin.createdAt <  :#{#filter.createdEndDateTime()})
            ORDER BY magasin.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(magasin)
            FROM Magasin magasin
            WHERE magasin.entreprise.id = :entrepriseId
              AND (:#{#filter.nom} IS NULL OR LOWER(magasin.nom) LIKE LOWER(CONCAT('%', :#{#filter.nom}, '%')))
              AND (:#{#filter.actif} IS NULL OR magasin.actif = :#{#filter.actif})
              AND (:#{#filter.createdStartDateTime()} IS NULL OR magasin.createdAt >= :#{#filter.createdStartDateTime()})
              AND (:#{#filter.createdEndDateTime()}   IS NULL OR magasin.createdAt <  :#{#filter.createdEndDateTime()})
            """)
    Page<MagasinResponse> findResponsesByFilter(@Param("filter") MagasinFilter filter,
                                                @Param("entrepriseId") UUID entrepriseId,
                                                Pageable pageable);
}
