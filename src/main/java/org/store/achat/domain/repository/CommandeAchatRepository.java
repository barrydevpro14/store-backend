package org.store.achat.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.achat.application.dto.CommandeAchatResponse;
import org.store.achat.domain.model.CommandeAchat;
import org.store.common.repository.BaseRepository;

import org.store.achat.domain.enums.CommandeAchatStatut;
import org.store.achat.domain.enums.StatutFacture;

import java.util.UUID;

public interface CommandeAchatRepository extends BaseRepository<CommandeAchat> {

    @Query("SELECT COUNT(commande) FROM CommandeAchat commande WHERE commande.magasin.id = :magasinId AND commande.statut = :statut")
    long countByMagasinIdAndStatut(@Param("magasinId") UUID magasinId, @Param("statut") CommandeAchatStatut statut);

    @Query("SELECT COUNT(commande) FROM CommandeAchat commande WHERE commande.magasin.entreprise.id = :entrepriseId AND commande.statut = :statut")
    long countByEntrepriseAndStatut(@Param("entrepriseId") UUID entrepriseId, @Param("statut") CommandeAchatStatut statut);

    /**
     * LEFT JOIN on facture is mandatory so that DRAFT orders (no facture yet) are included.
     * An implicit INNER JOIN via commande.facture.statut in WHERE would exclude all DRAFTs.
     */
    @Query("""
            SELECT new org.store.achat.application.dto.CommandeAchatResponse(commande)
            FROM CommandeAchat commande
            LEFT JOIN commande.facture facture
            WHERE commande.magasin.entreprise.id = :entrepriseId
              AND commande.magasin.id = :magasinId
              AND (:fournisseurId IS NULL OR commande.fournisseur.id = :fournisseurId)
              AND (:statut IS NULL OR commande.statut = :statut)
              AND (:statutFacture IS NULL OR facture.statut = :statutFacture)
              AND (:reference IS NULL OR :reference = '' OR LOWER(facture.numero) LIKE LOWER(CONCAT('%', :reference, '%'))
              OR LOWER(
                     CONCAT(
                         COALESCE(commande.fournisseur.nom, ''),' ',
                         COALESCE(commande.fournisseur.prenom, '')
                     )
                 ) LIKE LOWER(CONCAT('%', :reference, '%')))
              AND (:startDate IS NULL OR :startDate = '' OR FUNCTION('DATE', commande.createdAt) >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR FUNCTION('DATE', commande.createdAt) <= CAST(:endDate AS date))
            ORDER BY commande.createdAt DESC
            """)
    Page<CommandeAchatResponse> findResponsesByFilter(
            @Param("entrepriseId") UUID entrepriseId,
            @Param("magasinId") UUID magasinId,
            @Param("fournisseurId") UUID fournisseurId,
            @Param("statut") CommandeAchatStatut statut,
            @Param("statutFacture") StatutFacture statutFacture,
            @Param("reference") String reference,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            Pageable pageable);

    @Query("SELECT COUNT(commande) > 0 FROM CommandeAchat commande WHERE commande.createdBy = :accountId")
    boolean existsByCreatedBy(@Param("accountId") String accountId);
}
