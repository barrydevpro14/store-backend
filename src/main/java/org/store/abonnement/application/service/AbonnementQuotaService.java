package org.store.abonnement.application.service;

import org.springframework.stereotype.Service;
import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.service.AbonnementDomainService;
import org.store.common.exceptions.BadArgumentException;
import org.store.magasin.domain.service.MagasinDomainService;
import org.store.users.domain.service.EmployeDomainService;

import java.util.Optional;
import java.util.UUID;

/**
 * Vérifie les quotas du plan d'abonnement actif avant toute création de ressource.
 *
 * Règle : si {@code nombreMax = 0}, le quota est illimité (pas de vérification).
 * Sinon, {@code count >= nombreMax} → exception {@code BadArgumentException}.
 *
 * En cas d'abonnement absent (géré par la garde globale {@code auth.subscription.required}),
 * aucune vérification n'est effectuée ici.
 */
@Service
public class AbonnementQuotaService {

    private final AbonnementDomainService abonnementDomainService;
    private final MagasinDomainService magasinDomainService;
    private final EmployeDomainService employeDomainService;

    public AbonnementQuotaService(AbonnementDomainService abonnementDomainService,
                                  MagasinDomainService magasinDomainService,
                                  EmployeDomainService employeDomainService) {
        this.abonnementDomainService = abonnementDomainService;
        this.magasinDomainService = magasinDomainService;
        this.employeDomainService = employeDomainService;
    }

    /** Vérifie que l'entreprise peut créer un magasin supplémentaire. */
    public void ensureMagasinQuota(UUID entrepriseId) {
        Optional<Abonnement> abonnement = abonnementDomainService.findCurrent(entrepriseId);
        if (abonnement.isEmpty()) return;

        int max = planOf(abonnement.get()).getNombreMagasinsMax();
        if (max <= 0) return;

        long count = magasinDomainService.countByEntrepriseId(entrepriseId);
        if (count >= max) {
            throw new BadArgumentException("abonnement.quota.magasins.exceeded", max, count);
        }
    }

    /** Vérifie que le magasin peut accueillir un employé supplémentaire selon le plan actif. */
    public void ensureEmployeQuota(UUID entrepriseId, UUID magasinId) {
        Optional<Abonnement> abonnement = abonnementDomainService.findCurrent(entrepriseId);
        if (abonnement.isEmpty()) return;

        int max = planOf(abonnement.get()).getNombreEmployesMax();
        if (max <= 0) return;

        long count = employeDomainService.countByMagasinId(magasinId);
        if (count >= max) {
            throw new BadArgumentException("abonnement.quota.employes.exceeded", max, count);
        }
    }

    private PlanAbonnement planOf(Abonnement abonnement) {
        return abonnement.getTypePlanAbonnement().getPlan();
    }
}
