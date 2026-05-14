package org.store.achat.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.achat.application.dto.CommandeAchatFilter;
import org.store.achat.application.dto.CommandeAchatResponse;
import org.store.achat.domain.model.CommandeAchat;
import org.store.common.repository.BaseRepository;

import java.util.UUID;

public interface CommandeAchatRepository extends BaseRepository<CommandeAchat> {

    @Query("""
            SELECT new org.store.achat.application.dto.CommandeAchatResponse(c)
            FROM CommandeAchat c
            WHERE c.magasin.entreprise.id = :entrepriseId
              AND c.magasin.id = :#{#filter.magasinId}
              AND (:#{#filter.fournisseurId} IS NULL OR c.fournisseur.id = :#{#filter.fournisseurId})
              AND (:#{#filter.fromDateTime()} IS NULL OR c.createdAt >= :#{#filter.fromDateTime()})
              AND (:#{#filter.toDateTime()} IS NULL OR c.createdAt <= :#{#filter.toDateTime()})
            ORDER BY c.createdAt DESC
            """)
    Page<CommandeAchatResponse> findResponsesByFilter(@Param("filter") CommandeAchatFilter filter,
                                                     @Param("entrepriseId") UUID entrepriseId,
                                                     Pageable pageable);
}
