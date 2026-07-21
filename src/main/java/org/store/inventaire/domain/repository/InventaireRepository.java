package org.store.inventaire.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.inventaire.application.dto.InventaireResponse;
import org.store.inventaire.domain.enums.InventaireStatut;
import org.store.inventaire.domain.enums.TypeInventaire;
import org.store.inventaire.domain.model.Inventaire;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface InventaireRepository extends BaseRepository<Inventaire> {

    @Query("SELECT COUNT(inventaire) > 0 FROM Inventaire inventaire WHERE inventaire.magasin.id = :magasinId AND inventaire.statut IN :statuts")
    boolean existsByMagasinIdAndStatutIn(@Param("magasinId") UUID magasinId, @Param("statuts") Collection<InventaireStatut> statuts);

    @Query("""
            SELECT new org.store.inventaire.application.dto.InventaireResponse(inventaire)
            FROM Inventaire inventaire
            WHERE inventaire.magasin.id = :magasinId
              AND inventaire.statut IN :statuts
            ORDER BY inventaire.createdAt DESC
            """)
    java.util.List<InventaireResponse> findActiveByMagasinId(@Param("magasinId") UUID magasinId,
                                                             @Param("statuts") Collection<InventaireStatut> statuts);


    @Query("""
            SELECT new org.store.inventaire.application.dto.InventaireResponse(inventaire)
            FROM Inventaire inventaire
            WHERE inventaire.magasin.entreprise.id = :entrepriseId
              AND inventaire.magasin.id = :magasinId
              AND (:statut IS NULL OR inventaire.statut = :statut)
              AND (:type   IS NULL OR inventaire.type   = :type)
              AND (:startDate IS NULL OR :startDate = '' OR inventaire.date >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR inventaire.date <= CAST(:endDate AS date))
            ORDER BY inventaire.date DESC, inventaire.createdAt DESC
            """)
    Page<InventaireResponse> findResponsesByFilter(
            @Param("entrepriseId") UUID entrepriseId,
            @Param("magasinId") UUID magasinId,
            @Param("statut") InventaireStatut statut,
            @Param("type") TypeInventaire type,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            Pageable pageable);

    @Query("""
            SELECT new org.store.inventaire.application.dto.InventaireResponse(inventaire)
            FROM Inventaire inventaire
            WHERE inventaire.id = :id
              AND inventaire.magasin.entreprise.id = :entrepriseId
            """)
    Optional<InventaireResponse> findResponseById(@Param("id") UUID id,
                                                  @Param("entrepriseId") UUID entrepriseId);
}
