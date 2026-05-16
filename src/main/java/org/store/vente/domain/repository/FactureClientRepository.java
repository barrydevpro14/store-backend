package org.store.vente.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.vente.application.dto.FactureClientFilter;
import org.store.vente.application.dto.FactureClientResponse;
import org.store.vente.domain.model.FactureClient;

import java.util.Optional;
import java.util.UUID;

public interface FactureClientRepository extends BaseRepository<FactureClient> {

    Optional<FactureClient> findByCommandeId(UUID commandeId);

    @Query("""
            SELECT new org.store.vente.application.dto.FactureClientResponse(f)
            FROM FactureClient f
            WHERE f.commande.magasin.entreprise.id = :entrepriseId
              AND f.commande.magasin.id = :#{#filter.magasinId}
              AND (:#{#filter.clientId} IS NULL OR f.commande.client.id = :#{#filter.clientId})
              AND (:#{#filter.statutAsEnum()} IS NULL OR f.statut = :#{#filter.statutAsEnum()})
              AND (:#{#filter.fromDateTime()} IS NULL OR f.createdAt >= :#{#filter.fromDateTime()})
              AND (:#{#filter.toDateTime()} IS NULL OR f.createdAt <= :#{#filter.toDateTime()})
            """)
    Page<FactureClientResponse> findResponsesByFilter(@Param("filter") FactureClientFilter filter,
                                                     @Param("entrepriseId") UUID entrepriseId,
                                                     Pageable pageable);

    @Query("""
            SELECT new org.store.vente.application.dto.FactureClientResponse(f)
            FROM FactureClient f
            WHERE f.id = :id
              AND f.commande.magasin.entreprise.id = :entrepriseId
            """)
    Optional<FactureClientResponse> findResponseById(@Param("id") UUID id,
                                                    @Param("entrepriseId") UUID entrepriseId);
}
