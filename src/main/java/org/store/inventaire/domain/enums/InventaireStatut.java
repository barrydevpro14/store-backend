package org.store.inventaire.domain.enums;

/**
 * Cycle de vie d'un inventaire physique :
 * <ul>
 *   <li>{@code EN_COURS} : saisie en cours, lignes ajoutables.</li>
 *   <li>{@code BILAN} : saisies figées, écarts consultables, en attente de validation.</li>
 *   <li>{@code CLOTURE} : ajustements stock appliqués automatiquement, terminal.</li>
 *   <li>{@code ANNULE} : abandon sans application des écarts, terminal.</li>
 * </ul>
 */
public enum InventaireStatut {
    EN_COURS, BILAN, CLOTURE, ANNULE
}
