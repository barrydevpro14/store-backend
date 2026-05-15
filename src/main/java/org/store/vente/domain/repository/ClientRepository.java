package org.store.vente.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.vente.application.dto.ClientResponse;
import org.store.vente.domain.model.Client;

import java.util.UUID;

public interface ClientRepository extends BaseRepository<Client> {

    @Query("""
            SELECT new org.store.vente.application.dto.ClientResponse(c)
            FROM Client c
            WHERE c.magasin.id = :magasinId
              AND (:nom IS NULL OR LOWER(c.nom) LIKE LOWER(CONCAT('%', :nom, '%')))
              AND (:prenom IS NULL OR LOWER(c.prenom) LIKE LOWER(CONCAT('%', :prenom, '%')))
            """)
    Page<ClientResponse> findResponsesByMagasinId(@Param("magasinId") UUID magasinId,
                                                  @Param("nom") String nom,
                                                  @Param("prenom") String prenom,
                                                  Pageable pageable);

    @Query("""
            SELECT new org.store.vente.application.dto.ClientResponse(c)
            FROM Client c
            WHERE c.magasin.entreprise.id = :entrepriseId
              AND (:nom IS NULL OR LOWER(c.nom) LIKE LOWER(CONCAT('%', :nom, '%')))
              AND (:prenom IS NULL OR LOWER(c.prenom) LIKE LOWER(CONCAT('%', :prenom, '%')))
            """)
    Page<ClientResponse> findResponsesByEntrepriseId(@Param("entrepriseId") UUID entrepriseId,
                                                     @Param("nom") String nom,
                                                     @Param("prenom") String prenom,
                                                     Pageable pageable);
}
