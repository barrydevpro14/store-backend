package org.store.entreprise.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.store.common.repository.BaseRepository;
import org.store.entreprise.application.dto.EntrepriseResponse;
import org.store.entreprise.domain.model.Entreprise;

public interface EntrepriseRepository extends BaseRepository<Entreprise> {

    @Query("""
            SELECT new org.store.entreprise.application.dto.EntrepriseResponse(entreprise)
            FROM Entreprise entreprise
            """)
    Page<EntrepriseResponse> findAllProjected(Pageable pageable);
}
