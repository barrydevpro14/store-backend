package org.store.inventaire.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.inventaire.application.dto.LigneInventaireResponse;
import org.store.inventaire.domain.model.LigneInventaire;

import java.util.List;
import java.util.UUID;

public interface LigneInventaireRepository extends BaseRepository<LigneInventaire> {

    @Query("""
            SELECT new org.store.inventaire.application.dto.LigneInventaireResponse(ligne)
            FROM LigneInventaire ligne
            WHERE ligne.inventaire.id = :inventaireId
            ORDER BY ligne.id ASC
            """)
    Page<LigneInventaireResponse> findResponsesByInventaireId(@Param("inventaireId") UUID inventaireId, Pageable pageable);

    @Query("""
            SELECT ligne FROM LigneInventaire ligne
            WHERE ligne.inventaire.id = :inventaireId
            """)
    List<LigneInventaire> findAllByInventaireId(@Param("inventaireId") UUID inventaireId);

    boolean existsByInventaireIdAndProductFournisseurId(UUID inventaireId, UUID productFournisseurId);
}
