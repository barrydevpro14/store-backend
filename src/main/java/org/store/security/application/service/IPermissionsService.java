package org.store.security.application.service;

import java.util.List;
import java.util.UUID;

public interface IPermissionsService {

    List<String> findAllByRoleId(UUID roleId);
}
