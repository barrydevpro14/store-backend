package org.store.security.domain.repository;

import org.store.common.repository.BaseRepository;
import org.store.security.domain.model.Role;

import java.util.Optional;

public interface RoleRepository extends BaseRepository<Role> {

    Optional<Role> findByLibelle(String libelle);
}
