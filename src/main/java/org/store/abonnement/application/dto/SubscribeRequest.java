package org.store.abonnement.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SubscribeRequest(
        @NotNull UUID planId
) {}
