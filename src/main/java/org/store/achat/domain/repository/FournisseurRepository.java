package org.store.achat.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.achat.application.dto.FournisseurResponse;
import org.store.achat.domain.model.Fournisseur;
import org.store.common.repository.BaseRepository;

import java.util.Optional;
import java.util.UUID;

public interface FournisseurRepository extends BaseRepository<Fournisseur> {

    @Query("""
            SELECT new org.store.achat.application.dto.FournisseurResponse(f)
            FROM Fournisseur f
            WHERE f.entreprise.id = :entrepriseId
            """)
    Page<FournisseurResponse> findResponsesByEntrepriseId(@Param("entrepriseId") UUID entrepriseId, Pageable pageable);

    Optional<Fournisseur> findByReferenceAndEntrepriseId(String reference, UUID entrepriseId);

    boolean existsByReferenceAndEntrepriseId(String reference, UUID entrepriseId);
}
