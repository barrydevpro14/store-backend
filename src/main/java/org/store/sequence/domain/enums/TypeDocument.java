package org.store.sequence.domain.enums;

public enum TypeDocument {

    FACTURE_CLIENT("FACT"),
    FACTURE_ACHAT("FACT"),
    COMMANDE_CLIENT("VTE"),
    COMMANDE_ACHAT("CMD");

    private final String fallbackBase;

    TypeDocument(String fallbackBase) {
        this.fallbackBase = fallbackBase;
    }

    public String getFallbackBase() {
        return fallbackBase;
    }
}
