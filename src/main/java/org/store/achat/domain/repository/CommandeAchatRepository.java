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
            SELECT new org.store.achat.application.dto.CommandeAchatResponse(commande)
            FROM CommandeAchat commande
            WHERE commande.magasin.entreprise.id = :entrepriseId
              AND commande.magasin.id = :#{#filter.magasinId}
              AND (:#{#filter.fournisseurId} IS NULL OR commande.fournisseur.id = :#{#filter.fournisseurId})
              AND (:#{#filter.fromDateTime()} IS NULL OR commande.createdAt >= :#{#filter.fromDateTime()})
              AND (:#{#filter.toDateTime()} IS NULL OR commande.createdAt <= :#{#filter.toDateTime()})
            ORDER BY commande.createdAt DESC
            """)
    Page<CommandeAchatResponse> findResponsesByFilter(@Param("filter") CommandeAchatFilter filter,
                                                     @Param("entrepriseId") UUID entrepriseId,
                                                     Pageable pageable);
}
