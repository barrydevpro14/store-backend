package org.store.security.application.enums;

public enum Claim {
    USER("userId"),
    ENTREPRISE("entrepriseId"),
    MAGASIN("magasinId"),
    ROLE("role"),
    USERNAME("username"),
    PERMISSIONS("permissions"),
    CURRENCY("currency");

    private final String key;

    Claim(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
