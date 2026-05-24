package org.store.vente.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.vente.application.dto.ClientResponse;
import org.store.vente.domain.model.Client;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ClientRepository extends BaseRepository<Client> {

    /**
     * Listing magasin-scope avec filtres optionnels nom / prenom + fenêtre
     * de création. Le pattern LIKE est PRÉ-CONSTRUIT côté domain service
     * (cf. {@link org.store.common.tools.LikePatternHelper}) — workaround
     * du bug d'inférence bytea Hibernate 7 sur PostgreSQL. ORDER BY
     * createdAt DESC pour faire remonter les nouveaux clients.
     */
    @Query(value = """
            SELECT new org.store.vente.application.dto.ClientResponse(client)
            FROM Client client
            WHERE client.magasin.id = :magasinId
              AND (:nomPattern IS NULL OR LOWER(client.nom) LIKE :nomPattern)
              AND (:prenomPattern IS NULL OR LOWER(client.prenom) LIKE :prenomPattern)
              AND client.createdAt >= :createdStart
              AND client.createdAt <  :createdEnd
            ORDER BY client.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(client)
            FROM Client client
            WHERE client.magasin.id = :magasinId
              AND (:nomPattern IS NULL OR LOWER(client.nom) LIKE :nomPattern)
              AND (:prenomPattern IS NULL OR LOWER(client.prenom) LIKE :prenomPattern)
              AND client.createdAt >= :createdStart
              AND client.createdAt <  :createdEnd
            """)
    Page<ClientResponse> findResponsesByMagasinId(@Param("magasinId") UUID magasinId,
                                                  @Param("nomPattern") String nomPattern,
                                                  @Param("prenomPattern") String prenomPattern,
                                                  @Param("createdStart") LocalDateTime createdStart,
                                                  @Param("createdEnd") LocalDateTime createdEnd,
                                                  Pageable pageable);

    @Query(value = """
            SELECT new org.store.vente.application.dto.ClientResponse(client)
            FROM Client client
            WHERE client.magasin.entreprise.id = :entrepriseId
              AND (:nomPattern IS NULL OR LOWER(client.nom) LIKE :nomPattern)
              AND (:prenomPattern IS NULL OR LOWER(client.prenom) LIKE :prenomPattern)
              AND client.createdAt >= :createdStart
              AND client.createdAt <  :createdEnd
            ORDER BY client.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(client)
            FROM Client client
            WHERE client.magasin.entreprise.id = :entrepriseId
              AND (:nomPattern IS NULL OR LOWER(client.nom) LIKE :nomPattern)
              AND (:prenomPattern IS NULL OR LOWER(client.prenom) LIKE :prenomPattern)
              AND client.createdAt >= :createdStart
              AND client.createdAt <  :createdEnd
            """)
    Page<ClientResponse> findResponsesByEntrepriseId(@Param("entrepriseId") UUID entrepriseId,
                                                     @Param("nomPattern") String nomPattern,
                                                     @Param("prenomPattern") String prenomPattern,
                                                     @Param("createdStart") LocalDateTime createdStart,
                                                     @Param("createdEnd") LocalDateTime createdEnd,
                                                     Pageable pageable);
}
