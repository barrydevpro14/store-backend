package org.store.produit.domain.enums;

public enum UniteMesureEnum {
    PIECE, SAC, KG, LITRE, METRE, METRE_CARRE, CARTON;

    public String code() {
        return name();
    }
}
