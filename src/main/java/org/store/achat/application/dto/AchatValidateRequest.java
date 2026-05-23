package org.store.achat.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Payload de {@code POST /api/v1/purchases/{commandeId}/validate}.
 * `facture` est requise (le validate matérialise la facture
 * fournisseur). `paiement` est optionnel : si fourni, le premier
 * paiement est enregistré dans la même transaction que la création
 * de la facture, et `facture.montantPaye` est mis à jour en
 * conséquence — évite un second aller-retour POST /paiements pour
 * les OWNERs qui règlent au moment de valider.
 */
public record AchatValidateRequest(
        @NotNull @Valid FactureAchatCreateRequest facture,
        @Valid PaiementAchatRequest paiement
) {
}
