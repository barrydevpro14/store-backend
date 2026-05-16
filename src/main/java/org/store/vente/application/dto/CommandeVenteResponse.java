package org.store.vente.application.dto;

import org.store.common.dto.UserSummaryResponse;
import org.store.common.tools.DateHelper;
import org.store.magasin.application.dto.MagasinSummaryResponse;
import org.store.vente.domain.enums.CommandeVenteStatut;
import org.store.vente.domain.model.CommandeVente;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CommandeVenteResponse(
        UUID id,
        String reference,
        CommandeVenteStatut statut,
        ClientSummaryResponse client,
        MagasinSummaryResponse magasin,
        UserSummaryResponse user,
        LocalDate dateVente,
        BigDecimal montantTotal,
        BigDecimal montantPaye,
        String createdAt
) {
    public CommandeVenteResponse(CommandeVente commande, UserSummaryResponse user) {
        this(
                commande.getId(),
                commande.getReference(),
                commande.getStatut(),
                commande.getClient() != null ? new ClientSummaryResponse(commande.getClient()) : null,
                new MagasinSummaryResponse(commande.getMagasin()),
                user,
                commande.getDate(),
                commande.getMontantTotal(),
                commande.getMontantPaye() != null ? commande.getMontantPaye() : BigDecimal.ZERO,
                DateHelper.format(commande.getCreatedAt())
        );
    }
}
