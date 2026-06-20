package org.store.security.domain.repository;

import org.store.common.repository.BaseRepository;
import org.store.security.domain.model.PermissionGroup;

import java.util.List;

public interface PermissionGroupRepository extends BaseRepository<PermissionGroup> {

    List<PermissionGroup> findAllWithPermissions();

    List<PermissionGroup> findGroupsWithAssignablePermissions();
}
