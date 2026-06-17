package org.store.property;

import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "alert")
public record AlertProperties(
        String daily
) {
}
