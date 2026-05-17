package org.store.abonnement.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.abonnement.application.dto.AbonnementFilter;
import org.store.abonnement.domain.enums.AbonnementStatut;
import org.store.abonnement.domain.model.Abonnement;
import org.store.common.repository.BaseRepository;

import java.util.Optional;
import java.util.UUID;

public interface AbonnementRepository extends BaseRepository<Abonnement> {

    @Query("""
            SELECT a
            FROM Abonnement a
            WHERE a.entreprise.id = :entrepriseId
              AND a.statut        = :statut
            ORDER BY a.dateFin DESC NULLS LAST, a.id DESC
            """)
    Optional<Abonnement> findFirstByEntrepriseAndStatut(@Param("entrepriseId") UUID entrepriseId,
                                                       @Param("statut") AbonnementStatut statut);

    @Query("""
            SELECT a
            FROM Abonnement a
            WHERE (:#{#filter.entrepriseId}    IS NULL OR a.entreprise.id = :#{#filter.entrepriseId})
              AND (:#{#filter.statutAsEnum()}  IS NULL OR a.statut        = :#{#filter.statutAsEnum()})
              AND (:#{#filter.planId}          IS NULL OR a.plan.id       = :#{#filter.planId})
            """)
    Page<Abonnement> findEntitiesByFilter(@Param("filter") AbonnementFilter filter, Pageable pageable);
}
