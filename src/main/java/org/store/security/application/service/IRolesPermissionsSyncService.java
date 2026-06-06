package org.store.security.application.service;

import org.store.security.application.dto.RbacSyncReport;

public interface IRolesPermissionsSyncService {

    RbacSyncReport sync();
}
