package org.store.security.application.service;

import org.store.security.application.dto.PermissionGroupResponse;

import java.util.List;

public interface IPermissionGroupService {

    /** Returns all permission groups with all their permissions — for ADMIN system role management. */
    List<PermissionGroupResponse> findAll();

    /** Returns only groups and permissions assignable to custom company roles. */
    List<PermissionGroupResponse> findAllForCustomRole();
}
