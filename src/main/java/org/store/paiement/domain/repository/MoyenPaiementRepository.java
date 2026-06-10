package org.store.paiement.domain.repository;

import org.store.common.repository.BaseRepository;
import org.store.paiement.domain.model.MoyenPaiement;

import java.util.List;
import java.util.Optional;

public interface MoyenPaiementRepository extends BaseRepository<MoyenPaiement> {

    Optional<MoyenPaiement> findByCode(String code);

    List<MoyenPaiement> findAllByActifTrue();
}
