package org.store.vente.domain.repository;

import org.store.common.repository.BaseRepository;
import org.store.vente.domain.model.PaiementVente;

import java.util.List;
import java.util.UUID;

public interface PaiementVenteRepository extends BaseRepository<PaiementVente> {

    List<PaiementVente> findAllByFactureId(UUID factureId);
}
