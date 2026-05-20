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

    /**
     * Listing entreprise-scope. ORDER BY createdAt DESC pour que les
     * nouveaux fournisseurs apparaissent en haut de page 1 — sans cet
     * ordre, l'insertion ne garantit pas la position et un user qui
     * vient de créer peut croire qu'aucun save n'a eu lieu (la ligne
     * a juste été poussée en page 2 / 3).
     */
    @Query("""
            SELECT new org.store.achat.application.dto.FournisseurResponse(fournisseur)
            FROM Fournisseur fournisseur
            WHERE fournisseur.entreprise.id = :entrepriseId
            ORDER BY fournisseur.createdAt DESC
            """)
    Page<FournisseurResponse> findResponsesByEntrepriseId(@Param("entrepriseId") UUID entrepriseId, Pageable pageable);

    Optional<Fournisseur> findByReferenceAndEntrepriseId(String reference, UUID entrepriseId);

    boolean existsByReferenceAndEntrepriseId(String reference, UUID entrepriseId);
}
