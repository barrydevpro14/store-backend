package org.store.vente.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.store.achat.domain.enums.StatutFacture;
import org.store.common.service.GlobalService;
import org.store.common.tools.ReferenceHelper;
import org.store.vente.application.dto.CaisseResumeFilter;
import org.store.vente.application.dto.FactureClientCreate;
import org.store.vente.application.dto.FactureClientFilter;
import org.store.vente.application.dto.FactureClientResponse;
import org.store.vente.domain.model.FactureClient;
import org.store.vente.domain.repository.FactureClientRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
public class FactureClientDomainService extends GlobalService<FactureClient, FactureClientRepository> {
    public FactureClientDomainService(FactureClientRepository repository) {
        super(repository);
    }

    /** Crée et persiste une facture client initiale (statut NON_PAYEE, montantPaye=0). */
    public FactureClient create(FactureClientCreate factureClientCreate) {
        FactureClient facture = new FactureClient();
        facture.setCommande(factureClientCreate.commande());
        facture.setNumero(factureClientCreate.numero());
        facture.setDate(factureClientCreate.date());
        facture.setDateEcheance(factureClientCreate.dateEcheance());
        facture.setMontantTotal(factureClientCreate.montantTotal());
        facture.setMontantPaye(BigDecimal.ZERO);
        facture.setStatut(StatutFacture.NON_PAYEE);
        return save(facture);
    }

    /** Génère un numéro de facture unique au format FAC-VTE-yyyyMMdd-HHmmssSSS. */
    public String generateNumero() {
        return ReferenceHelper.generate("FAC-VTE");
    }

    public Optional<FactureClient> findByCommandeId(UUID commandeId) {
        return repository.findByCommandeId(commandeId);
    }

    /** Listing paginé filtré scopé entreprise (projection JPQL). */
    public Page<FactureClientResponse> findResponsesByFilter(FactureClientFilter filter, UUID entrepriseId) {
        return repository.findResponsesByFilter(filter, entrepriseId, filter.toPageable());
    }

    /** Projection JPQL d'une facture par id, scopée entreprise (Optional empty si introuvable ou autre entreprise). */
    public Optional<FactureClientResponse> findResponseById(UUID id, UUID entrepriseId) {
        return repository.findResponseById(id, entrepriseId);
    }

    /** Somme des montants totaux des factures rattachées à des commandes créées dans le magasin sur la période du range. */
    public BigDecimal sumMontantCommandesForCaisse(CaisseResumeFilter range, UUID entrepriseId) {
        return repository.sumMontantTotalByMagasinAndDay(range.magasinId(), entrepriseId, range.startOfPeriod(), range.endOfPeriod());
    }

    /** Incrémente montantPaye et recalcule le statut selon le rapport montantPaye/montantTotal. */
    public FactureClient applyPaiement(FactureClient facture, BigDecimal montant) {
        BigDecimal montantPayeActuel = facture.getMontantPaye() != null ? facture.getMontantPaye() : BigDecimal.ZERO;
        BigDecimal nouveauPaye = montantPayeActuel.add(montant);
        facture.setMontantPaye(nouveauPaye);

        if (nouveauPaye.compareTo(BigDecimal.ZERO) == 0) {
            facture.setStatut(StatutFacture.NON_PAYEE);
        } else if (nouveauPaye.compareTo(facture.getMontantTotal()) < 0) {
            facture.setStatut(StatutFacture.PARTIELLEMENT_PAYEE);
        } else {
            facture.setStatut(StatutFacture.PAYEE);
        }

        return save(facture);
    }
}
