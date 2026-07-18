package org.store.entreprise.domain.repository;

import org.store.common.repository.BaseRepository;
import org.store.entreprise.domain.model.EntrepriseSetting;

import java.util.Optional;
import java.util.UUID;

public interface EntrepriseSettingRepository extends BaseRepository<EntrepriseSetting> {
    Optional<EntrepriseSetting> findByEntrepriseId(UUID entrepriseId);
}
