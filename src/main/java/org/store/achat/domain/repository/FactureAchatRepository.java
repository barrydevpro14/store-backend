package org.store.achat.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.achat.application.dto.FactureAchatEcheanceFilter;
import org.store.achat.application.dto.FactureAchatFilter;
import org.store.achat.application.dto.FactureAchatResponse;
import org.store.achat.domain.model.FactureAchat;
import org.store.common.repository.BaseRepository;

import java.util.Optional;
import java.util.UUID;

public interface FactureAchatRepository extends BaseRepository<FactureAchat> {

    Optional<FactureAchat> findByCommandeId(UUID commandeId);

    boolean existsByNumero(String numero);

    @Query("""
            SELECT new org.store.achat.application.dto.FactureAchatResponse(facture)
            FROM FactureAchat facture
            WHERE facture.commande.magasin.entreprise.id = :entrepriseId
              AND facture.commande.magasin.id = :#{#filter.magasinId}
              AND (:#{#filter.fournisseurId} IS NULL OR facture.commande.fournisseur.id = :#{#filter.fournisseurId})
              AND (:#{#filter.statutAsEnum()} IS NULL OR facture.statut = :#{#filter.statutAsEnum()})
              AND (:#{#filter.fromDateTime()} IS NULL OR facture.createdAt >= :#{#filter.fromDateTime()})
              AND (:#{#filter.toDateTime()} IS NULL OR facture.createdAt <= :#{#filter.toDateTime()})
              AND (:#{#filter.createdStartDate} IS NULL OR FUNCTION('DATE', facture.createdAt) >= :#{#filter.createdStartDate})
              AND (:#{#filter.createdEndDate}   IS NULL OR FUNCTION('DATE', facture.createdAt) <  :#{#filter.createdEndDate})
            ORDER BY facture.createdAt DESC
            """)
    Page<FactureAchatResponse> findResponsesByFilter(@Param("filter") FactureAchatFilter filter,
                                                    @Param("entrepriseId") UUID entrepriseId,
                                                    Pageable pageable);

    @Query("""
            SELECT new org.store.achat.application.dto.FactureAchatResponse(facture)
            FROM FactureAchat facture
            WHERE facture.commande.magasin.entreprise.id = :entrepriseId
              AND facture.commande.magasin.id = :#{#filter.magasinId}
              AND facture.statut IN (org.store.achat.domain.enums.StatutFacture.NON_PAYEE, org.store.achat.domain.enums.StatutFacture.PARTIELLEMENT_PAYEE)
              AND facture.dateEcheance IS NOT NULL
              AND (:#{#filter.fromDate()} IS NULL OR facture.dateEcheance >= :#{#filter.fromDate()})
              AND (:#{#filter.toDate()} IS NULL OR facture.dateEcheance <= :#{#filter.toDate()})
            ORDER BY facture.dateEcheance ASC
            """)
    Page<FactureAchatResponse> findEcheances(@Param("filter") FactureAchatEcheanceFilter filter,
                                             @Param("entrepriseId") UUID entrepriseId,
                                             Pageable pageable);

    /** Finds unpaid purchase invoices whose due date is one of the given alert dates (today+1, today+3, today+5). */
    @Query("SELECT f FROM FactureAchat f WHERE f.statut = 'NON_PAYEE' AND f.dateEcheance IN :dates")
    java.util.List<FactureAchat> findDueOnDates(@Param("dates") java.util.List<java.time.LocalDate> dates);
}
