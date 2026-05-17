package org.store.abonnement.application.service;

import org.store.abonnement.domain.enums.ReductionType;
import org.store.common.exceptions.BadArgumentException;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Règles de validation transverses au module abonnement, réutilisées par les services
 * `SubscriptionType`, `Coupon` et `Promotion` qui partagent le couple (`reductionType`, `valeurReduction`)
 * et la fenêtre temporelle (`dateDebut` / `dateFin`).
 */
public final class SubscriptionRules {

    private static final BigDecimal POURCENTAGE_MAX = new BigDecimal("100");

    private SubscriptionRules() {
    }

    /**
     * Vérifie que `reductionType` et `valeurReduction` sont cohérents : l'un sans l'autre est interdit,
     * un POURCENTAGE supérieur à 100 est interdit. Lève `BadArgumentException(invalidKey)` sinon.
     */
    public static void ensureReductionConsistent(ReductionType reductionType, BigDecimal valeurReduction, String invalidKey) {
        boolean typeMissing = reductionType == null && valeurReduction != null;
        boolean valueMissing = reductionType != null && valeurReduction == null;
        boolean percentageTooHigh = reductionType == ReductionType.POURCENTAGE
                && valeurReduction != null && valeurReduction.compareTo(POURCENTAGE_MAX) > 0;

        if (typeMissing || valueMissing || percentageTooHigh) {
            throw new BadArgumentException(invalidKey);
        }
    }

    /**
     * Vérifie que `dateFin` est postérieure ou égale à `dateDebut`. Lève `BadArgumentException(invalidPeriodKey)` sinon.
     */
    public static void ensurePeriodValid(LocalDate dateDebut, LocalDate dateFin, String invalidPeriodKey) {
        if (dateFin.isBefore(dateDebut)) {
            throw new BadArgumentException(invalidPeriodKey);
        }
    }
}
