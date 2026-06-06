package org.store.security.application.service;

import org.store.security.application.dto.PermissionResponse;

import java.util.List;
import java.util.UUID;

public interface IPermissionsService {

    List<String> findAllByRoleId(UUID roleId);

    List<PermissionResponse> findAll();
}
