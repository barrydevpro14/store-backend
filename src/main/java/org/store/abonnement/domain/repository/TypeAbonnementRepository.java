package org.store.abonnement.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.abonnement.application.dto.SubscriptionTypeFilter;
import org.store.abonnement.application.dto.SubscriptionTypeResponse;
import org.store.abonnement.domain.model.TypeAbonnement;
import org.store.common.repository.BaseRepository;

import java.util.List;
import java.util.Optional;

public interface TypeAbonnementRepository extends BaseRepository<TypeAbonnement> {

    boolean existsByNom(String nom);

    Optional<TypeAbonnement> findByNom(String nom);

    @Query("""
            SELECT new org.store.abonnement.application.dto.SubscriptionTypeResponse(type)
            FROM TypeAbonnement type
            WHERE (:#{#filter.nom}        IS NULL OR LOWER(type.nom) LIKE LOWER(CONCAT('%', :#{#filter.nom}, '%')))
              AND (:#{#filter.actif}      IS NULL OR type.actif      = :#{#filter.actif})
              AND (:#{#filter.recommande} IS NULL OR type.recommande = :#{#filter.recommande})
            """)
    Page<SubscriptionTypeResponse> findResponsesByFilter(@Param("filter") SubscriptionTypeFilter filter, Pageable pageable);

    @Query("""
            SELECT new org.store.abonnement.application.dto.SubscriptionTypeResponse(type)
            FROM TypeAbonnement type
            WHERE type.actif = true
            ORDER BY type.ordre ASC, type.dureeMois ASC
            """)
    List<SubscriptionTypeResponse> findAllActifResponses();
}
