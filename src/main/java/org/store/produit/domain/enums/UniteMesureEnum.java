package org.store.produit.domain.enums;

public enum UniteMesureEnum {
    PIECE, SAC, KG, LITRE, METRE, METRE_CARRE, CARTON,
    METRE_CUBE, SACHET, CENTIMETRE, MILLIMETRE, GRAMME;

    public String code() {
        return name();
    }
}
