package org.store.common.dto;

public record ImageDownloadResponse(
        byte[] content,
        String contentType
) {
}
