package org.store.common.tools;

public final class NameHelper {

    private NameHelper() {
    }

    /** Concatène nom et prenom en gérant les null/blancs (jamais "null" littéral en sortie, jamais d'espace orphelin). */
    public static String formatNomComplet(String nom, String prenom) {
        String nomSafe = nom == null ? "" : nom.trim();
        String prenomSafe = prenom == null ? "" : prenom.trim();
        if (prenomSafe.isEmpty()) return nomSafe;
        if (nomSafe.isEmpty()) return prenomSafe;
        return nomSafe + " " + prenomSafe;
    }
}
