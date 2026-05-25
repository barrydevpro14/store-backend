package org.store.inventaire.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.inventaire.application.dto.InventaireFilter;
import org.store.inventaire.application.dto.InventaireResponse;
import org.store.inventaire.domain.model.Inventaire;

import java.util.Optional;
import java.util.UUID;

public interface InventaireRepository extends BaseRepository<Inventaire> {

    @Query("""
            SELECT new org.store.inventaire.application.dto.InventaireResponse(inventaire)
            FROM Inventaire inventaire
            WHERE inventaire.magasin.entreprise.id = :entrepriseId
              AND inventaire.magasin.id = :#{#filter.magasinId}
              AND (:#{#filter.statutAsEnum()} IS NULL OR inventaire.statut = :#{#filter.statutAsEnum()})
              AND (:#{#filter.fromDate()} IS NULL OR inventaire.date >= :#{#filter.fromDate()})
              AND (:#{#filter.toDate()} IS NULL OR inventaire.date <= :#{#filter.toDate()})
              AND (:#{#filter.createdStartDate} IS NULL OR inventaire.createdAt >= :#{#filter.createdStartDate})
              AND (:#{#filter.createdEndDate}   IS NULL OR inventaire.createdAt < :#{#filter.createdEndDate.plusDays(1)})
            ORDER BY inventaire.date DESC, inventaire.createdAt DESC
            """)
    Page<InventaireResponse> findResponsesByFilter(@Param("filter") InventaireFilter filter,
                                                  @Param("entrepriseId") UUID entrepriseId,
                                                  Pageable pageable);

    @Query("""
            SELECT new org.store.inventaire.application.dto.InventaireResponse(inventaire)
            FROM Inventaire inventaire
            WHERE inventaire.id = :id
              AND inventaire.magasin.entreprise.id = :entrepriseId
            """)
    Optional<InventaireResponse> findResponseById(@Param("id") UUID id,
                                                  @Param("entrepriseId") UUID entrepriseId);
}
