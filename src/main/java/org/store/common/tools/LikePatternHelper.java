package org.store.common.tools;

/**
 * Helper de construction de patterns SQL `LIKE` pour les recherches
 * text insensibles à la casse.
 *
 * <p>Pourquoi ce helper existe : sur PostgreSQL + Hibernate 7, un
 * paramètre nommé `:term` utilisé dans deux contextes
 * (`:term IS NULL` ET `LIKE LOWER(CONCAT('%', :term, '%'))`) peut voir
 * son type JDBC inféré sur `bytea` au lieu de `varchar`. La requête
 * échoue alors avec `lower(bytea) does not exist`. Précomputer le
 * pattern côté Java et le passer en bind direct (`LIKE :pattern`)
 * verrouille le type bind sur `String` (varchar) et élimine
 * l'ambiguïté.
 *
 * <p>Convention d'usage côté repository : remplacer
 * <pre>
 *   AND (:term IS NULL OR LOWER(col) LIKE LOWER(CONCAT('%', :term, '%')))
 * </pre>
 * par
 * <pre>
 *   AND (:termPattern IS NULL OR LOWER(col) LIKE :termPattern)
 * </pre>
 * et utiliser {@link #toLikePattern(String)} côté domain service pour
 * construire le bind.
 */
public final class LikePatternHelper {

    private LikePatternHelper() {
        // utility — instanciation interdite
    }

    /**
     * Construit un pattern LIKE `%term%` en lower-case prêt à être
     * binder. Retourne `null` si l'input est `null` ou blank — le JPQL
     * appelant traite ce `null` comme "filtre désactivé" via
     * `:pattern IS NULL OR ...`.
     */
    public static String toLikePattern(String term) {
        if (term == null || term.isBlank()) {
            return null;
        }
        return "%" + term.toLowerCase() + "%";
    }
}
