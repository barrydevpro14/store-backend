package org.store.magasin.application.dto;

/** Per-store employee stats row for the admin reporting view. */
public record MagasinStatsRow(
        String  nom,
        boolean actif,
        long    employesActifs,
        long    employesInactifs
) {}
