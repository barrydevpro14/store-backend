package org.store.abonnement.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.store.abonnement.application.dto.PaiementAbonnementFilter;
import org.store.abonnement.application.dto.PaiementAbonnementRequest;
import org.store.abonnement.application.dto.PaiementAbonnementResponse;
import org.store.abonnement.application.dto.SubscriptionAmountBreakdown;
import org.store.abonnement.application.service.impl.PaiementAbonnementCreationContext;
import org.store.abonnement.domain.enums.StatutPaiementAbonnement;
import org.store.abonnement.domain.model.PaiementAbonnement;
import org.store.abonnement.domain.repository.PaiementAbonnementRepository;
import org.store.common.service.GlobalService;

import java.util.UUID;

@Service
public class PaiementAbonnementDomainService extends GlobalService<PaiementAbonnement, PaiementAbonnementRepository> {
    public PaiementAbonnementDomainService(PaiementAbonnementRepository repository) {
        super(repository);
    }

    public PaiementAbonnement createPending(PaiementAbonnementCreationContext context) {
        PaiementAbonnementRequest paiementAbonnementRequest = context.paiementAbonnementRequest();
        SubscriptionAmountBreakdown breakdown = context.breakdown();

        PaiementAbonnement paiement = new PaiementAbonnement();
        paiement.setAbonnement(context.abonnement());
        paiement.setMontantAvantReduction(breakdown.prixDeBase());
        paiement.setReduction(breakdown.reductionType()
                .add(breakdown.reductionPromotion())
                .add(breakdown.reductionCoupon()));
        paiement.setMontantFinal(breakdown.montantAPayer());
        paiement.setDatePaiement(paiementAbonnementRequest.datePaiement());
        paiement.setMoyen(paiementAbonnementRequest.moyenAsEnum());
        paiement.setReferenceTransaction(paiementAbonnementRequest.referenceTransaction());
        paiement.setStatut(StatutPaiementAbonnement.EN_ATTENTE_VALIDATION);
        paiement.setPreuve(context.preuve());
        return save(paiement);
    }

    public boolean existsPendingForAbonnement(UUID abonnementId) {
        return repository.existsByAbonnementIdAndStatut(abonnementId, StatutPaiementAbonnement.EN_ATTENTE_VALIDATION);
    }

    public Page<PaiementAbonnementResponse> findResponses(PaiementAbonnementFilter filter) {
        return repository.findResponsesByFilter(filter);
    }

    public PaiementAbonnement markAsValide(PaiementAbonnement paiement) {
        paiement.setStatut(StatutPaiementAbonnement.VALIDE);
        return save(paiement);
    }

    public PaiementAbonnement markAsRejete(PaiementAbonnement paiement, String motifRejet) {
        paiement.setStatut(StatutPaiementAbonnement.REJETE);
        paiement.setMotifRejet(motifRejet);
        return save(paiement);
    }
}
