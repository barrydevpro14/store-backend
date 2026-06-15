package org.store.vente.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.achat.domain.enums.StatutFacture;
import org.store.common.repository.BaseRepository;
import org.store.vente.application.dto.CommandeVenteResponse;
import org.store.vente.application.dto.VenteParVendeurResponse;
import org.store.vente.domain.enums.CommandeVenteStatut;
import org.store.vente.domain.model.CommandeVente;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommandeVenteRepository extends BaseRepository<CommandeVente> {

    /**
     * LEFT JOIN on facture is mandatory so that DRAFT orders (no facture yet) are included.
     * LEFT JOIN on account resolves createdBy (String) → user for vendeur filtering.
     */
    @Query("""
            SELECT new org.store.vente.application.dto.CommandeVenteResponse(commande, facture.statut, facture.montantPaye)
            FROM CommandeVente commande
            LEFT JOIN commande.facture facture
            LEFT JOIN org.store.security.domain.model.Account account ON CAST(account.id AS string) = commande.createdBy
            WHERE commande.magasin.entreprise.id = :entrepriseId
              AND commande.magasin.id = :magasinId
              AND (:clientId IS NULL OR commande.client.id = :clientId)
              AND (:vendeurId IS NULL OR account.user.id = :vendeurId)
              AND (:statut IS NULL OR commande.statut = :statut)
              AND (:statutFacture IS NULL OR facture.statut = :statutFacture)
              AND (:reference IS NULL OR :reference = '' OR LOWER(commande.reference) LIKE LOWER(CONCAT('%', :reference, '%')))
              AND (:montantMin IS NULL OR commande.montantTotal >= :montantMin)
              AND (:montantMax IS NULL OR commande.montantTotal <= :montantMax)
              AND (:startDate IS NULL OR :startDate = '' OR FUNCTION('DATE', commande.createdAt) >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR FUNCTION('DATE', commande.createdAt) <= CAST(:endDate AS date))
            ORDER BY commande.createdAt DESC
            """)
    Page<CommandeVenteResponse> findResponsesByFilter(
            @Param("entrepriseId") UUID entrepriseId,
            @Param("magasinId") UUID magasinId,
            @Param("clientId") UUID clientId,
            @Param("vendeurId") UUID vendeurId,
            @Param("statut") CommandeVenteStatut statut,
            @Param("statutFacture") StatutFacture statutFacture,
            @Param("reference") String reference,
            @Param("montantMin") BigDecimal montantMin,
            @Param("montantMax") BigDecimal montantMax,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            Pageable pageable);

    @Query("""
            SELECT new org.store.vente.application.dto.CommandeVenteResponse(
                commande, user.id, TRIM(BOTH FROM CONCAT(COALESCE(user.nom, ''), ' ', COALESCE(user.prenom, ''))),
                facture.montantPaye
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
            SELECT COUNT(commande) FROM CommandeVente commande
            WHERE commande.magasin.entreprise.id = :entrepriseId
              AND commande.statut = org.store.vente.domain.enums.CommandeVenteStatut.VALIDATE
              AND commande.createdAt >= :startOfDay
              AND commande.createdAt <= :endOfDay
            """)
    long countByEntrepriseAndDay(@Param("entrepriseId") UUID entrepriseId,
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
    List<VenteParVendeurResponse> ventilationParVendeurByMagasinAndDay(
            @Param("magasinId") UUID magasinId,
            @Param("entrepriseId") UUID entrepriseId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay);

    @Query("SELECT COUNT(commande) > 0 FROM CommandeVente commande WHERE commande.createdBy = :accountId")
    boolean existsByCreatedBy(@Param("accountId") String accountId);
}
