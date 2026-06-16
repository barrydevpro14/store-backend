package org.store.achat.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.achat.application.dto.FactureAchatResponse;
import org.store.achat.domain.enums.StatutFacture;
import org.store.achat.domain.model.FactureAchat;
import org.store.common.repository.BaseRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FactureAchatRepository extends BaseRepository<FactureAchat> {

    Optional<FactureAchat> findByCommandeId(UUID commandeId);

    boolean existsByNumero(String numero);

    @Query("""
            SELECT new org.store.achat.application.dto.FactureAchatResponse(facture)
            FROM FactureAchat facture
            WHERE facture.commande.magasin.entreprise.id = :entrepriseId
              AND facture.commande.magasin.id = :magasinId
              AND (:fournisseurId IS NULL OR facture.commande.fournisseur.id = :fournisseurId)
              AND (:statut IS NULL OR facture.statut = :statut)
              AND (:startDate IS NULL OR :startDate = '' OR FUNCTION('DATE', facture.createdAt) >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR FUNCTION('DATE', facture.createdAt) <= CAST(:endDate AS date))
            ORDER BY facture.createdAt DESC
            """)
    Page<FactureAchatResponse> findResponsesByFilter(
            @Param("entrepriseId") UUID entrepriseId,
            @Param("magasinId") UUID magasinId,
            @Param("fournisseurId") UUID fournisseurId,
            @Param("statut") StatutFacture statut,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            Pageable pageable);

    @Query("""
            SELECT new org.store.achat.application.dto.FactureAchatResponse(facture)
            FROM FactureAchat facture
            WHERE facture.commande.magasin.entreprise.id = :entrepriseId
              AND facture.commande.magasin.id = :magasinId
              AND facture.statut IN (org.store.achat.domain.enums.StatutFacture.NON_PAYEE, org.store.achat.domain.enums.StatutFacture.PARTIELLEMENT_PAYEE)
              AND facture.dateEcheance IS NOT NULL
              AND (:startDate IS NULL OR :startDate = '' OR facture.dateEcheance >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR facture.dateEcheance <= CAST(:endDate AS date))
            ORDER BY facture.dateEcheance ASC
            """)
    Page<FactureAchatResponse> findEcheances(
            @Param("entrepriseId") UUID entrepriseId,
            @Param("magasinId") UUID magasinId,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            Pageable pageable);

    /** Finds unpaid purchase invoices whose due date is one of the given alert dates (today+1, today+3, today+5). */
    @Query("SELECT f FROM FactureAchat f WHERE f.statut = 'NON_PAYEE' AND f.dateEcheance IN :dates")
    List<FactureAchat> findDueOnDates(@Param("dates") List<LocalDate> dates);
}
