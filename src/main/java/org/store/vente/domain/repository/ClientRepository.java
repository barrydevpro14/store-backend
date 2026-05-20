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

    /**
     * Listing magasin-scope avec filtres optionnels nom / prenom.
     *
     * Le pattern LIKE est PRÉ-CONSTRUIT côté domain service (Java) puis
     * passé en bind direct (`:nomPattern`) au lieu de `LIKE
     * LOWER(CONCAT('%', :nom, '%'))`. Raison : sur PostgreSQL avec
     * Hibernate 7, le paramètre nommé bare `:nom` utilisé deux fois
     * (`IS NULL` + `CONCAT`) déclenche une inférence de type qui peut
     * binder le paramètre en bytea — d'où `lower(bytea) does not
     * exist`. Passer un String pré-formé verrouille le type texte.
     *
     * ORDER BY createdAt DESC pour que les nouveaux clients apparaissent
     * en haut de page 1 — sans ordre, l'insertion ne garantit aucune
     * position et un user qui vient de créer un client peut croire
     * qu'aucun save n'a eu lieu (le client a juste été poussé en page 2).
     */
    @Query("""
            SELECT new org.store.vente.application.dto.ClientResponse(client)
            FROM Client client
            WHERE client.magasin.id = :magasinId
              AND (:nomPattern IS NULL OR LOWER(client.nom) LIKE :nomPattern)
              AND (:prenomPattern IS NULL OR LOWER(client.prenom) LIKE :prenomPattern)
            ORDER BY client.createdAt DESC
            """)
    Page<ClientResponse> findResponsesByMagasinId(@Param("magasinId") UUID magasinId,
                                                  @Param("nomPattern") String nomPattern,
                                                  @Param("prenomPattern") String prenomPattern,
                                                  Pageable pageable);

    @Query("""
            SELECT new org.store.vente.application.dto.ClientResponse(client)
            FROM Client client
            WHERE client.magasin.entreprise.id = :entrepriseId
              AND (:nomPattern IS NULL OR LOWER(client.nom) LIKE :nomPattern)
              AND (:prenomPattern IS NULL OR LOWER(client.prenom) LIKE :prenomPattern)
            ORDER BY client.createdAt DESC
            """)
    Page<ClientResponse> findResponsesByEntrepriseId(@Param("entrepriseId") UUID entrepriseId,
                                                     @Param("nomPattern") String nomPattern,
                                                     @Param("prenomPattern") String prenomPattern,
                                                     Pageable pageable);
}
