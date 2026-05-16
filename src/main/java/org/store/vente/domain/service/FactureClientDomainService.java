package org.store.vente.domain.service;

import org.springframework.stereotype.Service;
import org.store.achat.domain.enums.StatutFacture;
import org.store.common.service.GlobalService;
import org.store.common.tools.ReferenceHelper;
import org.store.vente.application.dto.FactureClientCreate;
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
        facture.setDateEcheache(factureClientCreate.dateEcheance());
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
