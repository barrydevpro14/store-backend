package org.store.vente.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.vente.application.dto.FactureClientFilter;
import org.store.vente.application.dto.FactureClientResponse;
import org.store.vente.domain.model.FactureClient;

import org.store.achat.domain.enums.StatutFacture;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FactureClientRepository extends BaseRepository<FactureClient> {

    @Query("SELECT COUNT(facture) FROM FactureClient facture WHERE facture.commande.magasin.id = :magasinId AND facture.statut = :statut")
    long countByMagasinIdAndStatut(@Param("magasinId") UUID magasinId, @Param("statut") StatutFacture statut);

    Optional<FactureClient> findByCommandeId(UUID commandeId);

    @Query("""
            SELECT new org.store.vente.application.dto.FactureClientResponse(facture)
            FROM FactureClient facture
            LEFT JOIN org.store.security.domain.model.Account account ON CAST(account.id AS string) = facture.createdBy
            WHERE facture.commande.magasin.entreprise.id = :entrepriseId
              AND facture.commande.magasin.id = :#{#filter.magasinId}
              AND (:#{#filter.clientId} IS NULL OR facture.commande.client.id = :#{#filter.clientId})
              AND (:#{#filter.vendeurId} IS NULL OR account.user.id = :#{#filter.vendeurId})
              AND (:#{#filter.statutAsEnum()} IS NULL OR facture.statut = :#{#filter.statutAsEnum()})
              AND (:#{#filter.numero} IS NULL OR LOWER(facture.numero) LIKE LOWER(CONCAT('%', :#{#filter.numero}, '%')))
              AND (:#{#filter.montantMin} IS NULL OR facture.montantTotal >= :#{#filter.montantMin})
              AND (:#{#filter.montantMax} IS NULL OR facture.montantTotal <= :#{#filter.montantMax})
              AND (:#{#filter.fromDateTime()} IS NULL OR facture.createdAt >= :#{#filter.fromDateTime()})
              AND (:#{#filter.toDateTime()} IS NULL OR facture.createdAt <= :#{#filter.toDateTime()})
              AND facture.createdAt >= :#{#filter.createdStartDateTime()}
              AND facture.createdAt <  :#{#filter.createdEndDateTime()}
            ORDER BY facture.createdAt DESC
            """)
    Page<FactureClientResponse> findResponsesByFilter(@Param("filter") FactureClientFilter filter,
                                                     @Param("entrepriseId") UUID entrepriseId,
                                                     Pageable pageable);

    @Query("""
            SELECT new org.store.vente.application.dto.FactureClientResponse(facture)
            FROM FactureClient facture
            WHERE facture.id = :id
              AND facture.commande.magasin.entreprise.id = :entrepriseId
            """)
    Optional<FactureClientResponse> findResponseById(@Param("id") UUID id,
                                                    @Param("entrepriseId") UUID entrepriseId);

    @Query("""
            SELECT COALESCE(SUM(facture.montantTotal), 0) FROM FactureClient facture
            WHERE facture.commande.magasin.entreprise.id = :entrepriseId
              AND facture.commande.statut = org.store.vente.domain.enums.CommandeVenteStatut.VALIDATE
              AND facture.commande.createdAt >= :startOfDay
              AND facture.commande.createdAt <= :endOfDay
            """)
    BigDecimal sumMontantTotalByEntrepriseAndDay(@Param("entrepriseId") UUID entrepriseId,
                                                 @Param("startOfDay") LocalDateTime startOfDay,
                                                 @Param("endOfDay") LocalDateTime endOfDay);

    @Query("""
            SELECT COUNT(facture) FROM FactureClient facture
            WHERE facture.commande.magasin.entreprise.id = :entrepriseId
              AND facture.statut IN :statuts
            """)
    long countByEntrepriseAndStatuts(@Param("entrepriseId") UUID entrepriseId,
                                     @Param("statuts") List<StatutFacture> statuts);

    @Query("""
            SELECT COALESCE(SUM(facture.montantTotal), 0) FROM FactureClient facture
            WHERE facture.commande.magasin.entreprise.id = :entrepriseId
              AND facture.commande.magasin.id = :magasinId
              AND facture.commande.statut = org.store.vente.domain.enums.CommandeVenteStatut.VALIDATE
              AND facture.commande.createdAt >= :startOfDay
              AND facture.commande.createdAt <= :endOfDay
            """)
    BigDecimal sumMontantTotalByMagasinAndDay(@Param("magasinId") UUID magasinId,
                                              @Param("entrepriseId") UUID entrepriseId,
                                              @Param("startOfDay") LocalDateTime startOfDay,
                                              @Param("endOfDay") LocalDateTime endOfDay);

    /** Finds unpaid sale invoices whose due date was exactly :daysAgo days ago (for 1/3/5-day overdue alerts). */
    @Query("SELECT f FROM FactureClient f WHERE f.statut = 'NON_PAYEE' AND f.dateEcheance = :dueDate")
    java.util.List<FactureClient> findOverdueByDueDate(@Param("dueDate") java.time.LocalDate dueDate);
}
