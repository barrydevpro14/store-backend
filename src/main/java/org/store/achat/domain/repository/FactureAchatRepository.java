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
            SELECT new org.store.achat.application.dto.FactureAchatResponse(f)
            FROM FactureAchat f
            WHERE f.commande.magasin.entreprise.id = :entrepriseId
              AND f.commande.magasin.id = :#{#filter.magasinId}
              AND (:#{#filter.fournisseurId} IS NULL OR f.commande.fournisseur.id = :#{#filter.fournisseurId})
              AND (:#{#filter.statutAsEnum()} IS NULL OR f.statut = :#{#filter.statutAsEnum()})
              AND (:#{#filter.fromDateTime()} IS NULL OR f.createdAt >= :#{#filter.fromDateTime()})
              AND (:#{#filter.toDateTime()} IS NULL OR f.createdAt <= :#{#filter.toDateTime()})
            ORDER BY f.createdAt DESC
            """)
    Page<FactureAchatResponse> findResponsesByFilter(@Param("filter") FactureAchatFilter filter,
                                                    @Param("entrepriseId") UUID entrepriseId,
                                                    Pageable pageable);

    @Query("""
            SELECT new org.store.achat.application.dto.FactureAchatResponse(f)
            FROM FactureAchat f
            WHERE f.commande.magasin.entreprise.id = :entrepriseId
              AND f.commande.magasin.id = :#{#filter.magasinId}
              AND f.statut IN (org.store.achat.domain.enums.StatutFacture.NON_PAYEE, org.store.achat.domain.enums.StatutFacture.PARTIELLEMENT_PAYEE)
              AND f.dateEcheance IS NOT NULL
              AND (:#{#filter.fromDate()} IS NULL OR f.dateEcheance >= :#{#filter.fromDate()})
              AND (:#{#filter.toDate()} IS NULL OR f.dateEcheance <= :#{#filter.toDate()})
            ORDER BY f.dateEcheance ASC
            """)
    Page<FactureAchatResponse> findEcheances(@Param("filter") FactureAchatEcheanceFilter filter,
                                             @Param("entrepriseId") UUID entrepriseId,
                                             Pageable pageable);
}
