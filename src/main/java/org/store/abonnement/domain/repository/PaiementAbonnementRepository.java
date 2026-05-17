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

import java.util.UUID;

public interface PaiementAbonnementRepository extends BaseRepository<PaiementAbonnement> {

    boolean existsByAbonnementIdAndStatut(UUID abonnementId, StatutPaiementAbonnement statut);

    @Query("""
            SELECT new org.store.abonnement.application.dto.PaiementAbonnementResponse(paiement)
            FROM PaiementAbonnement paiement
            JOIN paiement.abonnement abonnement
            WHERE (:statut       IS NULL OR paiement.statut    = :statut)
              AND (:abonnementId IS NULL OR abonnement.id      = :abonnementId)
              AND (:entrepriseId IS NULL OR abonnement.entreprise.id = :entrepriseId)
            """)
    Page<PaiementAbonnementResponse> findResponsesByFilter(@Param("statut") StatutPaiementAbonnement statut,
                                                           @Param("abonnementId") UUID abonnementId,
                                                           @Param("entrepriseId") UUID entrepriseId,
                                                           Pageable pageable);

    default Page<PaiementAbonnementResponse> findResponsesByFilter(PaiementAbonnementFilter filter) {
        return findResponsesByFilter(filter.statutAsEnum(), filter.abonnementId(), filter.entrepriseId(), filter.toPageable());
    }
}
