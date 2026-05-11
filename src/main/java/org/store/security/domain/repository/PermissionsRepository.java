package org.store.security.domain.repository;

import org.store.common.repository.BaseRepository;
import org.store.security.domain.model.Permissions;

import java.util.Optional;

public interface PermissionsRepository extends BaseRepository<Permissions> {

    Optional<Permissions> findByCode(String code);
}
