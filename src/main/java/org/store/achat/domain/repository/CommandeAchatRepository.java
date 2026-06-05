package org.store.achat.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.achat.application.dto.CommandeAchatFilter;
import org.store.achat.application.dto.CommandeAchatResponse;
import org.store.achat.domain.model.CommandeAchat;
import org.store.common.repository.BaseRepository;

import org.store.achat.domain.enums.CommandeAchatStatut;

import java.util.UUID;

public interface CommandeAchatRepository extends BaseRepository<CommandeAchat> {

    @Query("SELECT COUNT(commande) FROM CommandeAchat commande WHERE commande.magasin.id = :magasinId AND commande.statut = :statut")
    long countByMagasinIdAndStatut(@Param("magasinId") UUID magasinId, @Param("statut") CommandeAchatStatut statut);

    @Query("SELECT COUNT(commande) FROM CommandeAchat commande WHERE commande.magasin.entreprise.id = :entrepriseId AND commande.statut = :statut")
    long countByEntrepriseAndStatut(@Param("entrepriseId") UUID entrepriseId, @Param("statut") CommandeAchatStatut statut);

    @Query("""
            SELECT new org.store.achat.application.dto.CommandeAchatResponse(commande)
            FROM CommandeAchat commande
            WHERE commande.magasin.entreprise.id = :entrepriseId
              AND commande.magasin.id = :#{#filter.magasinId}
              AND (:#{#filter.fournisseurId} IS NULL OR commande.fournisseur.id = :#{#filter.fournisseurId})
              AND (:#{#filter.statutAsEnum()} IS NULL OR commande.statut = :#{#filter.statutAsEnum()})
              AND (:#{#filter.statutFactureAsEnum()} IS NULL OR commande.facture.statut = :#{#filter.statutFactureAsEnum()})
              AND (:#{#filter.reference} IS NULL OR LOWER(commande.reference) LIKE LOWER(CONCAT('%', :#{#filter.reference}, '%')))
              AND (:#{#filter.fromDateTime()} IS NULL OR commande.createdAt >= :#{#filter.fromDateTime()})
              AND (:#{#filter.toDateTime()} IS NULL OR commande.createdAt <= :#{#filter.toDateTime()})
              AND commande.createdAt >= :#{#filter.createdStartDateTime()}
              AND commande.createdAt <  :#{#filter.createdEndDateTime()}
            ORDER BY commande.createdAt DESC
            """)
    Page<CommandeAchatResponse> findResponsesByFilter(@Param("filter") CommandeAchatFilter filter,
                                                     @Param("entrepriseId") UUID entrepriseId,
                                                     Pageable pageable);
}
