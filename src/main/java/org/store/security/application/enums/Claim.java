package org.store.security.application.enums;

public enum Claim {
    ENTREPRISE("entrepriseId"),
    MAGASIN("magasinId"),
    USERNAME("username"),
    PERMISSIONS("permissions");

    private final String key;

    Claim(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
