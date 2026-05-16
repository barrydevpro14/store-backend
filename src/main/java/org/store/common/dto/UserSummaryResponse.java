package org.store.common.dto;

import java.util.UUID;

public record UserSummaryResponse(
        UUID id,
        String nomComplet
) {
}
