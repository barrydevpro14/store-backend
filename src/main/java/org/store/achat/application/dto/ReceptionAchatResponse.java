package org.store.achat.application.dto;

import org.store.achat.domain.enums.CommandeAchatStatut;

import java.util.UUID;

public record ReceptionAchatResponse(
        UUID commandeId,
        String reference,
        CommandeAchatStatut statut,
        int totalQuantiteRecueDansCetteReception,
        int totalQuantiteRecueGlobale,
        int totalQuantiteCommandee
) {
}
