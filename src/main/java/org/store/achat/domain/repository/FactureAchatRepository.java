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

    @Query("""
            SELECT new org.store.achat.application.dto.FactureAchatResponse(facture)
            FROM FactureAchat facture
            WHERE facture.commande.magasin.entreprise.id = :entrepriseId
              AND facture.commande.magasin.id = :#{#filter.magasinId}
              AND (:#{#filter.fournisseurId} IS NULL OR facture.commande.fournisseur.id = :#{#filter.fournisseurId})
              AND (:#{#filter.statutAsEnum()} IS NULL OR facture.statut = :#{#filter.statutAsEnum()})
              AND (:#{#filter.fromDateTime()} IS NULL OR facture.createdAt >= :#{#filter.fromDateTime()})
              AND (:#{#filter.toDateTime()} IS NULL OR facture.createdAt <= :#{#filter.toDateTime()})
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
}
