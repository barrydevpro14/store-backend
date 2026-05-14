package org.store.achat.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.achat.application.dto.PaiementAchatResponse;
import org.store.achat.domain.model.PaiementAchat;
import org.store.common.repository.BaseRepository;

import java.util.UUID;

public interface PaiementAchatRepository extends BaseRepository<PaiementAchat> {

    @Query("""
            SELECT new org.store.achat.application.dto.PaiementAchatResponse(p)
            FROM PaiementAchat p
            WHERE p.facture.id = :factureId
            ORDER BY p.datePaiement DESC
            """)
    Page<PaiementAchatResponse> findResponsesByFactureId(@Param("factureId") UUID factureId, Pageable pageable);
}
