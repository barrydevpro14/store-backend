package org.store.achat.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Payload de {@code POST /api/v1/achats/{commandeId}/receive}.
 * `facture` est requise (la réception matérialise la facture
 * fournisseur en même temps que les entrées stock). `paiement` est
 * optionnel : si fourni, le premier paiement est enregistré dans la
 * même transaction que la création de la facture, et
 * `facture.montantPaye` est mis à jour en conséquence — évite un
 * second aller-retour POST /paiements pour les OWNERs qui règlent à
 * la réception.
 */
public record AchatReceiveRequest(
        @NotNull @Valid FactureAchatCreateRequest facture,
        @Valid PaiementAchatRequest paiement
) {
}
