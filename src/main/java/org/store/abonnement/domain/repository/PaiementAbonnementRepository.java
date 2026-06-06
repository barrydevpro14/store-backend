package org.store.abonnement.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.abonnement.application.dto.PaiementAbonnementFilter;
import org.store.abonnement.application.dto.PaiementAbonnementResponse;
import org.store.abonnement.domain.enums.StatutPaiementAbonnement;
import org.store.abonnement.domain.model.PaiementAbonnement;
import org.store.common.repository.BaseRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface PaiementAbonnementRepository extends BaseRepository<PaiementAbonnement> {

    boolean existsByAbonnementIdAndStatut(UUID abonnementId, StatutPaiementAbonnement statut);

    @Query("SELECT COUNT(p) FROM PaiementAbonnement p WHERE p.statut = :statut")
    long countByStatut(@Param("statut") StatutPaiementAbonnement statut);

    @Query("""
            SELECT COALESCE(SUM(p.montantFinal), 0)
            FROM PaiementAbonnement p
            WHERE p.statut = org.store.abonnement.domain.enums.StatutPaiementAbonnement.VALIDE
              AND p.datePaiement >= :startOfYear
              AND p.datePaiement <  :startOfNextYear
            """)
    BigDecimal sumValidatedRevenueForYear(@Param("startOfYear") LocalDate startOfYear,
                                          @Param("startOfNextYear") LocalDate startOfNextYear);

    @Query(value = """
            SELECT new org.store.abonnement.application.dto.PaiementAbonnementResponse(paiement)
            FROM PaiementAbonnement paiement
            LEFT JOIN FETCH paiement.abonnement abonnement
            LEFT JOIN FETCH abonnement.entreprise
            LEFT JOIN FETCH abonnement.typePlanAbonnement type
            LEFT JOIN FETCH type.plan
            WHERE (:#{#filter.statutAsEnum()}        IS NULL OR paiement.statut         = :#{#filter.statutAsEnum()})
              AND (:#{#filter.abonnementId}          IS NULL OR abonnement.id           = :#{#filter.abonnementId})
              AND (:#{#filter.entrepriseId}          IS NULL OR abonnement.entreprise.id = :#{#filter.entrepriseId})
              AND paiement.createdAt >= :#{#filter.createdStartDateTime()}
              AND paiement.createdAt <  :#{#filter.createdEndDateTime()}
            ORDER BY paiement.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(paiement)
            FROM PaiementAbonnement paiement
            JOIN paiement.abonnement abonnement
            WHERE (:#{#filter.statutAsEnum()}        IS NULL OR paiement.statut         = :#{#filter.statutAsEnum()})
              AND (:#{#filter.abonnementId}          IS NULL OR abonnement.id           = :#{#filter.abonnementId})
              AND (:#{#filter.entrepriseId}          IS NULL OR abonnement.entreprise.id = :#{#filter.entrepriseId})
              AND paiement.createdAt >= :#{#filter.createdStartDateTime()}
              AND paiement.createdAt <  :#{#filter.createdEndDateTime()}
            """)
    Page<PaiementAbonnementResponse> findResponsesByFilter(@Param("filter") PaiementAbonnementFilter filter,
                                                           Pageable pageable);
}
