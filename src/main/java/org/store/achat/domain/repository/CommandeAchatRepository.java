package org.store.achat.domain.repository;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.achat.domain.model.CommandeAchat;
import org.store.common.repository.BaseRepository;

import org.store.achat.domain.enums.CommandeAchatStatut;

import java.util.UUID;

public interface CommandeAchatRepository extends BaseRepository<CommandeAchat>, JpaSpecificationExecutor<CommandeAchat> {

    @Query("SELECT COUNT(c) FROM CommandeAchat c WHERE c.magasin.id = :magasinId AND c.statut = :statut")
    long countByMagasinIdAndStatut(@Param("magasinId") UUID magasinId, @Param("statut") CommandeAchatStatut statut);

    @Query("SELECT COUNT(c) FROM CommandeAchat c WHERE c.magasin.entreprise.id = :entrepriseId AND c.statut = :statut")
    long countByEntrepriseAndStatut(@Param("entrepriseId") UUID entrepriseId, @Param("statut") CommandeAchatStatut statut);

    @Query("SELECT COUNT(c) > 0 FROM CommandeAchat c WHERE c.createdBy = :accountId")
    boolean existsByCreatedBy(@Param("accountId") String accountId);
}
