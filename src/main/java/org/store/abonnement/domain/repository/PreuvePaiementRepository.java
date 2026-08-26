package org.store.abonnement.domain.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.abonnement.domain.enums.StatutPreuvePaiement;
import org.store.abonnement.domain.model.PreuvePaiement;
import org.store.common.repository.BaseRepository;

import java.util.List;
import java.util.UUID;

public interface PreuvePaiementRepository extends BaseRepository<PreuvePaiement> {

    boolean existsByPaiementAbonnementIdAndStatut(UUID paiementAbonnementId, StatutPreuvePaiement statut);

    @Query("""
            SELECT preuve FROM PreuvePaiement preuve
            LEFT JOIN FETCH preuve.moyen
            WHERE preuve.paiementAbonnement.id = :paiementAbonnementId
            ORDER BY preuve.createdAt DESC
            """)
    List<PreuvePaiement> findByPaiementAbonnementIdOrderByCreatedAtDesc(@Param("paiementAbonnementId") UUID paiementAbonnementId);
}
