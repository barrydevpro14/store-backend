package org.store.common.dto;

import java.util.Arrays;
import java.util.Objects;

public record ImageDownloadResponse(
        byte[] content,
        String contentType
) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ImageDownloadResponse other)) return false;
        return Arrays.equals(content, other.content) && Objects.equals(contentType, other.contentType);
    }

    @Override
    public int hashCode() {
        return 31 * Arrays.hashCode(content) + Objects.hashCode(contentType);
    }

    @Override
    public String toString() {
        int size = content != null ? content.length : 0;
        return "ImageDownloadResponse[content=" + size + " bytes, contentType=" + contentType + "]";
    }
}
