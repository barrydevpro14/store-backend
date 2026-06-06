package org.store.security.application.dto;

import java.util.List;

public record RbacSyncReport(
        List<String> addedPermissions,
        List<String> addedRoles,
        List<String> updatedRoles,
        List<String> orphanPermissions,
        List<String> orphanRoles
) {
}
