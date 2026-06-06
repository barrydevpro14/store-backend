package org.store.contact.application.dto;

import jakarta.validation.constraints.NotBlank;

public record ContactReplyRequest(
        @NotBlank String reponse
) {
}
