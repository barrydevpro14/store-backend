package org.store.entreprise.application.dto;

import java.util.UUID;

/** Lightweight entreprise row for combobox / select use cases. */
public record EntrepriseSelectItem(
        UUID   id,
        String sigle,
        String raisonSociale,
        String activiteEconomique
) {}
