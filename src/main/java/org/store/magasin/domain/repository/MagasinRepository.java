package org.store.magasin.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.magasin.application.dto.MagasinResponse;
import org.store.magasin.domain.model.Magasin;

import java.util.UUID;

public interface MagasinRepository extends BaseRepository<Magasin> {

    @Query("""
            SELECT new org.store.magasin.application.dto.MagasinResponse(m.id, m.nom, m.adresse, m.actif, m.entreprise.id)
            FROM Magasin m
            WHERE m.entreprise.id = :entrepriseId
            """)
    Page<MagasinResponse> findResponsesByEntrepriseId(@Param("entrepriseId") UUID entrepriseId, Pageable pageable);
}
