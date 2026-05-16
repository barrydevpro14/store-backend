package org.store.vente.domain.repository;

import org.store.common.repository.BaseRepository;
import org.store.vente.domain.model.FactureClient;

import java.util.Optional;
import java.util.UUID;

public interface FactureClientRepository extends BaseRepository<FactureClient> {

    Optional<FactureClient> findByCommandeId(UUID commandeId);
}
