package org.store.abonnement.domain.enums;

public enum PeriodiciteAbonnement {

    MENSUEL(1),
    BIMENSUEL(2),
    TRIMESTRIEL(3),
    ANNUEL(12);

    private final int nombreMois;

    PeriodiciteAbonnement(int nombreMois) {
        this.nombreMois = nombreMois;
    }

    public int getNombreMois() {
        return nombreMois;
    }
}
