package org.store.achat.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.achat.domain.model.LigneCommandeAchat;

import java.util.UUID;

public interface LigneCommandeAchatRepository extends BaseRepository<LigneCommandeAchat> {

    @Query("SELECT l FROM LigneCommandeAchat l WHERE l.commande.id = :commandeId ORDER BY l.id ASC")
    Page<LigneCommandeAchat> findPagedByCommandeId(@Param("commandeId") UUID commandeId, Pageable pageable);
}
