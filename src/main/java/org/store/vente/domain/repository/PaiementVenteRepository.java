package org.store.vente.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.vente.application.dto.PaiementVenteResponse;
import org.store.vente.domain.model.PaiementVente;

import java.util.List;
import java.util.UUID;

public interface PaiementVenteRepository extends BaseRepository<PaiementVente> {

    List<PaiementVente> findAllByFactureId(UUID factureId);

    @Query("""
            SELECT new org.store.vente.application.dto.PaiementVenteResponse(p)
            FROM PaiementVente p
            WHERE p.facture.id = :factureId
              AND p.facture.commande.magasin.entreprise.id = :entrepriseId
            """)
    Page<PaiementVenteResponse> findResponsesByFactureId(@Param("factureId") UUID factureId,
                                                        @Param("entrepriseId") UUID entrepriseId,
                                                        Pageable pageable);
}
