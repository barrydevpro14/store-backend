package org.store.property;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

@ConfigurationProperties(prefix = "security.rbac")
public record RbacProperties(
        boolean sync,
        Resource file,
        String adminPassword
) {
}
