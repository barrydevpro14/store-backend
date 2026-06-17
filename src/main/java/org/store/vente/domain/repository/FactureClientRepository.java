package org.store.vente.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.vente.application.dto.FactureClientResponse;
import org.store.vente.domain.model.FactureClient;

import org.store.achat.domain.enums.StatutFacture;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FactureClientRepository extends BaseRepository<FactureClient> {

    @Query("SELECT COUNT(facture) FROM FactureClient facture WHERE facture.commande.magasin.id = :magasinId AND facture.statut IN :statuts")
    long countByMagasinIdAndStatut(@Param("magasinId") UUID magasinId, @Param("statuts") List<StatutFacture> statuts);

    Optional<FactureClient> findByCommandeId(UUID commandeId);

    @Query("""
            SELECT new org.store.vente.application.dto.FactureClientResponse(facture)
            FROM FactureClient facture
            LEFT JOIN org.store.security.domain.model.Account account ON CAST(account.id AS string) = facture.createdBy
            WHERE facture.commande.magasin.entreprise.id = :entrepriseId
              AND facture.commande.magasin.id = :magasinId
              AND (:clientId IS NULL OR facture.commande.client.id = :clientId)
              AND (:vendeurId IS NULL OR account.user.id = :vendeurId)
              AND (:statut IS NULL OR facture.statut = :statut)
              AND (:numero IS NULL OR :numero = '' OR facture.numero ilike CONCAT('%', :numero, '%'))
              AND (:montantMin IS NULL OR facture.montantTotal >= :montantMin)
              AND (:montantMax IS NULL OR facture.montantTotal <= :montantMax)
              AND (:startDate IS NULL OR :startDate = '' OR FUNCTION('DATE', facture.createdAt) >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR FUNCTION('DATE', facture.createdAt) <= CAST(:endDate AS date))
            ORDER BY facture.createdAt DESC
            """)
    Page<FactureClientResponse> findResponsesByFilter(
            @Param("entrepriseId") UUID entrepriseId,
            @Param("magasinId") UUID magasinId,
            @Param("clientId") UUID clientId,
            @Param("vendeurId") UUID vendeurId,
            @Param("statut") StatutFacture statut,
            @Param("numero") String numero,
            @Param("montantMin") BigDecimal montantMin,
            @Param("montantMax") BigDecimal montantMax,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
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

    /** Finds unpaid sale invoices whose due date is one of the given alert dates (today+1, today+3, today+5). */
    @Query("""
    SELECT f
    FROM FactureClient f
    JOIN FETCH f.commande commande
    JOIN FETCH commande.magasin
    WHERE f.dateEcheance IN :dates
    AND f.statut IN :statutFactures
""")
    List<FactureClient> findDueOnDates(@Param("dates") List<LocalDate> dates , List<StatutFacture> statutFactures);
}
