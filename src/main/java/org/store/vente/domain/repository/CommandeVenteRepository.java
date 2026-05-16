package org.store.vente.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.vente.application.dto.CommandeVenteFilter;
import org.store.vente.application.dto.CommandeVenteResponse;
import org.store.vente.domain.model.CommandeVente;

import java.util.Optional;
import java.util.UUID;

public interface CommandeVenteRepository extends BaseRepository<CommandeVente> {

    @Query("""
            SELECT new org.store.vente.application.dto.CommandeVenteResponse(c)
            FROM CommandeVente c
            LEFT JOIN org.store.security.domain.model.Account a ON CAST(a.id AS string) = c.createdBy
            WHERE c.magasin.entreprise.id = :entrepriseId
              AND c.magasin.id = :#{#filter.magasinId}
              AND (:#{#filter.clientId} IS NULL OR c.client.id = :#{#filter.clientId})
              AND (:#{#filter.vendeurId} IS NULL OR a.user.id = :#{#filter.vendeurId})
              AND (:#{#filter.statutAsEnum()} IS NULL OR c.statut = :#{#filter.statutAsEnum()})
              AND (:#{#filter.reference} IS NULL OR LOWER(c.reference) LIKE LOWER(CONCAT('%', :#{#filter.reference}, '%')))
              AND (:#{#filter.montantMin} IS NULL OR c.montantTotal >= :#{#filter.montantMin})
              AND (:#{#filter.montantMax} IS NULL OR c.montantTotal <= :#{#filter.montantMax})
              AND (:#{#filter.fromDateTime()} IS NULL OR c.createdAt >= :#{#filter.fromDateTime()})
              AND (:#{#filter.toDateTime()} IS NULL OR c.createdAt <= :#{#filter.toDateTime()})
            """)
    Page<CommandeVenteResponse> findResponsesByFilter(@Param("filter") CommandeVenteFilter filter,
                                                     @Param("entrepriseId") UUID entrepriseId,
                                                     Pageable pageable);

    @Query("""
            SELECT new org.store.vente.application.dto.CommandeVenteResponse(
                c, u.id, TRIM(BOTH FROM CONCAT(COALESCE(u.nom, ''), ' ', COALESCE(u.prenom, '')))
            )
            FROM CommandeVente c
            LEFT JOIN org.store.security.domain.model.Account a ON CAST(a.id AS string) = c.createdBy
            LEFT JOIN a.user u
            WHERE c.id = :id
              AND c.magasin.entreprise.id = :entrepriseId
            """)
    Optional<CommandeVenteResponse> findResponseById(@Param("id") UUID id,
                                                    @Param("entrepriseId") UUID entrepriseId);
}
