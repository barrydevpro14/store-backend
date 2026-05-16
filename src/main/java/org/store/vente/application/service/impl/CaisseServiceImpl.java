package org.store.vente.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.service.ValidatorService;
import org.store.magasin.application.service.IMagasinService;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;
import org.store.vente.application.dto.CaisseResumeFilter;
import org.store.vente.application.dto.CaisseResumeResponse;
import org.store.vente.application.dto.PaiementParMoyenResponse;
import org.store.vente.application.dto.TopProduitResponse;
import org.store.vente.application.dto.TopProduitsFilter;
import org.store.vente.application.dto.VenteParVendeurResponse;
import org.store.vente.application.service.ICaisseService;
import org.store.vente.domain.service.CommandeVenteDomainService;
import org.store.vente.domain.service.FactureClientDomainService;
import org.store.vente.domain.service.LigneCommandeVenteDomainService;
import org.store.vente.domain.service.PaiementVenteDomainService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Agrège le résumé de caisse d'un magasin pour une journée : compte les commandes,
 * somme les quantités vendues et les montants commande + paiements. Le scoping
 * entreprise est appliqué dans chaque query JPQL ; l'accès magasin du caller est
 * validé via {@link IMagasinService}.
 */
@Service
@Transactional(readOnly = true)
public class CaisseServiceImpl implements ICaisseService {

    private final CommandeVenteDomainService commandeVenteDomainService;
    private final FactureClientDomainService factureClientDomainService;
    private final LigneCommandeVenteDomainService ligneCommandeVenteDomainService;
    private final PaiementVenteDomainService paiementVenteDomainService;
    private final IMagasinService magasinService;
    private final ICurrentUserService currentUserService;
    private final ValidatorService validatorService;

    public CaisseServiceImpl(CommandeVenteDomainService commandeVenteDomainService,
                             FactureClientDomainService factureClientDomainService,
                             LigneCommandeVenteDomainService ligneCommandeVenteDomainService,
                             PaiementVenteDomainService paiementVenteDomainService,
                             IMagasinService magasinService,
                             ICurrentUserService currentUserService,
                             ValidatorService validatorService) {
        this.commandeVenteDomainService = commandeVenteDomainService;
        this.factureClientDomainService = factureClientDomainService;
        this.ligneCommandeVenteDomainService = ligneCommandeVenteDomainService;
        this.paiementVenteDomainService = paiementVenteDomainService;
        this.magasinService = magasinService;
        this.currentUserService = currentUserService;
        this.validatorService = validatorService;
    }

    /** Valide le filter, vérifie l'accès magasin, lance les 4 queries agrégées (3 sur commandes + 1 sur paiements) et combine. */
    @Override
    public CaisseResumeResponse getResume(CaisseResumeFilter filter) {
        validatorService.validate(filter);
        UserPrincipal currentUser = currentUserService.getCurrent();
        magasinService.ensureAccessibleByCurrentUser(magasinService.findById(filter.magasinId()));

        UUID entrepriseId = currentUser.entrepriseId();
        long nombreCommandes = commandeVenteDomainService.countCommandesForCaisse(filter, entrepriseId);
        long nombreProduits = commandeVenteDomainService.sumQuantiteProduitsForCaisse(filter, entrepriseId);
        BigDecimal totalCommandes = factureClientDomainService.sumMontantCommandesForCaisse(filter, entrepriseId);
        BigDecimal totalPaiements = paiementVenteDomainService.sumPaiementsForCaisse(filter, entrepriseId);
        List<PaiementParMoyenResponse> paiementsParMoyen = paiementVenteDomainService.ventilationParMoyenForCaisse(filter, entrepriseId);
        List<VenteParVendeurResponse> ventesParVendeur = commandeVenteDomainService.ventilationParVendeurForCaisse(filter, entrepriseId);

        return new CaisseResumeResponse(
                filter.magasinId(), filter.dateAsLocalDate(),
                nombreCommandes, nombreProduits, totalCommandes, totalPaiements,
                paiementsParMoyen, ventesParVendeur
        );
    }

    /** Top N produits les plus vendus (par quantité) dans le magasin sur la journée du filter. */
    @Override
    public List<TopProduitResponse> findTopProduits(TopProduitsFilter filter) {
        validatorService.validate(filter);
        UserPrincipal currentUser = currentUserService.getCurrent();
        magasinService.ensureAccessibleByCurrentUser(magasinService.findById(filter.magasinId()));
        return ligneCommandeVenteDomainService.findTopProduitsForCaisse(filter, currentUser.entrepriseId());
    }
}
