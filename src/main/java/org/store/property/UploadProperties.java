package org.store.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Set;
import java.util.stream.Collectors;

@ConfigurationProperties(prefix = "upload")
public record UploadProperties(
        Set<String> allowedImageTypes
) {
    public UploadProperties {
        allowedImageTypes = allowedImageTypes == null
                ? Set.of()
                : allowedImageTypes.stream()
                        .map(String::toLowerCase)
                        .collect(Collectors.toUnmodifiableSet());
    }
}
