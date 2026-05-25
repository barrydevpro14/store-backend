package org.store.vente.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.vente.application.dto.CommandeVenteFilter;
import org.store.vente.application.dto.CommandeVenteResponse;
import org.store.vente.domain.model.CommandeVente;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface CommandeVenteRepository extends BaseRepository<CommandeVente> {

    @Query("""
            SELECT new org.store.vente.application.dto.CommandeVenteResponse(commande, facture.montantTotal, facture.montantPaye)
            FROM CommandeVente commande
            LEFT JOIN commande.facture facture
            LEFT JOIN org.store.security.domain.model.Account account ON CAST(account.id AS string) = commande.createdBy
            WHERE commande.magasin.entreprise.id = :entrepriseId
              AND commande.magasin.id = :#{#filter.magasinId}
              AND (:#{#filter.clientId} IS NULL OR commande.client.id = :#{#filter.clientId})
              AND (:#{#filter.vendeurId} IS NULL OR account.user.id = :#{#filter.vendeurId})
              AND (:#{#filter.statutAsEnum()} IS NULL OR commande.statut = :#{#filter.statutAsEnum()})
              AND (:#{#filter.reference} IS NULL OR LOWER(commande.reference) LIKE LOWER(CONCAT('%', :#{#filter.reference}, '%')))
              AND (:#{#filter.montantMin} IS NULL OR facture.montantTotal >= :#{#filter.montantMin})
              AND (:#{#filter.montantMax} IS NULL OR facture.montantTotal <= :#{#filter.montantMax})
              AND (:#{#filter.fromDateTime()} IS NULL OR commande.createdAt >= :#{#filter.fromDateTime()})
              AND (:#{#filter.toDateTime()} IS NULL OR commande.createdAt <= :#{#filter.toDateTime()})
              AND commande.createdAt >= :#{#filter.createdStartDateTime()}
              AND commande.createdAt <  :#{#filter.createdEndDateTime()}
            ORDER BY commande.createdAt DESC
            """)
    Page<CommandeVenteResponse> findResponsesByFilter(@Param("filter") CommandeVenteFilter filter,
                                                     @Param("entrepriseId") UUID entrepriseId,
                                                     Pageable pageable);

    @Query("""
            SELECT new org.store.vente.application.dto.CommandeVenteResponse(
                commande, user.id, TRIM(BOTH FROM CONCAT(COALESCE(user.nom, ''), ' ', COALESCE(user.prenom, ''))),
                facture.montantTotal, facture.montantPaye
            )
            FROM CommandeVente commande
            LEFT JOIN commande.facture facture
            LEFT JOIN org.store.security.domain.model.Account account ON CAST(account.id AS string) = commande.createdBy
            LEFT JOIN account.user user
            WHERE commande.id = :id
              AND commande.magasin.entreprise.id = :entrepriseId
            """)
    Optional<CommandeVenteResponse> findResponseById(@Param("id") UUID id,
                                                    @Param("entrepriseId") UUID entrepriseId);

    @Query("""
            SELECT COUNT(commande) FROM CommandeVente commande
            WHERE commande.magasin.entreprise.id = :entrepriseId
              AND commande.magasin.id = :magasinId
              AND commande.statut = org.store.vente.domain.enums.CommandeVenteStatut.VALIDATE
              AND commande.createdAt >= :startOfDay
              AND commande.createdAt <= :endOfDay
            """)
    long countByMagasinAndDay(@Param("magasinId") UUID magasinId,
                              @Param("entrepriseId") UUID entrepriseId,
                              @Param("startOfDay") LocalDateTime startOfDay,
                              @Param("endOfDay") LocalDateTime endOfDay);

    @Query("""
            SELECT COALESCE(SUM(ligne.quantite), 0) FROM LigneCommandeVente ligne
            WHERE ligne.commande.magasin.entreprise.id = :entrepriseId
              AND ligne.commande.magasin.id = :magasinId
              AND ligne.commande.statut = org.store.vente.domain.enums.CommandeVenteStatut.VALIDATE
              AND ligne.commande.createdAt >= :startOfDay
              AND ligne.commande.createdAt <= :endOfDay
            """)
    long sumQuantiteLignesByMagasinAndDay(@Param("magasinId") UUID magasinId,
                                          @Param("entrepriseId") UUID entrepriseId,
                                          @Param("startOfDay") LocalDateTime startOfDay,
                                          @Param("endOfDay") LocalDateTime endOfDay);

    @Query("""
            SELECT new org.store.vente.application.dto.VenteParVendeurResponse(
                user.id,
                TRIM(BOTH FROM CONCAT(COALESCE(user.nom, ''), ' ', COALESCE(user.prenom, ''))),
                COUNT(commande),
                COALESCE(SUM(facture.montantTotal), 0)
            )
            FROM CommandeVente commande
            LEFT JOIN commande.facture facture
            LEFT JOIN org.store.security.domain.model.Account account ON CAST(account.id AS string) = commande.createdBy
            LEFT JOIN account.user user
            WHERE commande.magasin.entreprise.id = :entrepriseId
              AND commande.magasin.id = :magasinId
              AND commande.statut = org.store.vente.domain.enums.CommandeVenteStatut.VALIDATE
              AND commande.createdAt >= :startOfDay
              AND commande.createdAt <= :endOfDay
              AND user.id IS NOT NULL
            GROUP BY user.id, user.nom, user.prenom
            ORDER BY COUNT(commande) DESC
            """)
    java.util.List<org.store.vente.application.dto.VenteParVendeurResponse> ventilationParVendeurByMagasinAndDay(
            @Param("magasinId") UUID magasinId,
            @Param("entrepriseId") UUID entrepriseId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay);
}
