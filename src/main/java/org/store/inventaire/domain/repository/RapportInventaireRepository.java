package org.store.inventaire.domain.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.inventaire.application.dto.RapportInventaireResponse;
import org.store.inventaire.domain.model.RapportInventaire;

import java.util.Optional;
import java.util.UUID;

public interface RapportInventaireRepository extends BaseRepository<RapportInventaire> {

    @Query("""
            SELECT new org.store.inventaire.application.dto.RapportInventaireResponse(rapport)
            FROM RapportInventaire rapport
            WHERE rapport.inventaire.id = :inventaireId
              AND rapport.inventaire.magasin.entreprise.id = :entrepriseId
            """)
    Optional<RapportInventaireResponse> findResponseByInventaireId(@Param("inventaireId") UUID inventaireId,
                                                                  @Param("entrepriseId") UUID entrepriseId);

    boolean existsByInventaireId(UUID inventaireId);
}
