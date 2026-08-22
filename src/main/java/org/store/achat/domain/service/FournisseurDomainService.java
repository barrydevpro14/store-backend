package org.store.achat.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.store.achat.application.dto.FournisseurFilter;
import org.store.common.exceptions.UniqueResourceException;
import org.store.achat.application.dto.FournisseurRequest;
import org.store.achat.application.dto.FournisseurResponse;
import org.store.achat.application.dto.FournisseurSearchSummaryResponse;
import org.store.achat.domain.model.Fournisseur;
import org.store.achat.domain.repository.FournisseurRepository;
import org.store.common.service.GlobalService;
import org.store.common.tools.LikePatternHelper;
import org.store.entreprise.domain.model.Entreprise;

import java.util.Optional;
import java.util.UUID;

@Service
public class FournisseurDomainService extends GlobalService<Fournisseur, FournisseurRepository> {
    public FournisseurDomainService(FournisseurRepository repository) {
        super(repository);
    }

    public Fournisseur create(FournisseurRequest fournisseurRequest, Entreprise entreprise) {
        Fournisseur fournisseur = new Fournisseur();
        fournisseur.setNom(fournisseurRequest.nom());
        fournisseur.setPrenom(fournisseurRequest.prenom());
        fournisseur.setEmail(fournisseurRequest.email());
        fournisseur.setTelephone(fournisseurRequest.telephone());
        fournisseur.setAdresse(fournisseurRequest.adresse());
        fournisseur.setReference(fournisseurRequest.reference());
        fournisseur.setOrigine(fournisseurRequest.origine());
        fournisseur.setEntreprise(entreprise);
        return save(fournisseur);
    }

    /** Crée (ou retrouve) le fournisseur système global "Anonyme" — unique pour toute la plateforme (entreprise = null). */
    public Fournisseur ensureGlobalAnonymous() {
        return repository.findGlobalByReference(Fournisseur.ANONYMOUS_REFERENCE)
                .orElseGet(() -> {
                    Fournisseur f = new Fournisseur();
                    f.setNom("Fournisseur anonyme");
                    f.setReference(Fournisseur.ANONYMOUS_REFERENCE);
                    f.setEntreprise(null);
                    f.setSysteme(true);
                    return save(f);
                });
    }

    public Page<FournisseurResponse> findResponsesByFilter(FournisseurFilter filter, UUID entrepriseId) {
        Page<FournisseurResponse> responsesByFilter = repository.findResponsesByFilter(
                entrepriseId,
                filter.nom(), LikePatternHelper.toLikePattern(filter.nom()),
                filter.reference(), LikePatternHelper.toLikePattern(filter.reference()),
                filter.startDate(), filter.endDate(),
                filter.toPageable());
        return responsesByFilter;
    }

    public Page<FournisseurSearchSummaryResponse> searchSummaries(UUID entrepriseId, String q, Pageable pageable) {
        return repository.searchSummaries(entrepriseId, LikePatternHelper.toLikePattern(q), pageable);
    }

    public Optional<Fournisseur> findByReferenceAndEntrepriseId(String reference, UUID entrepriseId) {
        return repository.findByReferenceAndEntrepriseId(reference, entrepriseId);
    }

    public boolean existsByReferenceAndEntrepriseId(String reference, UUID entrepriseId) {
        return repository.existsByReferenceAndEntrepriseId(reference, entrepriseId);
    }

    /** Vérifie que email/telephone (si renseignés) ne sont pas déjà utilisés par un autre fournisseur de la même entreprise. */
    public void ensureOptionalContactsInEntreprise(String email, String telephone, UUID entrepriseId) {
        if (StringUtils.hasText(email) && repository.existsByEmailAndEntrepriseId(email, entrepriseId)) {
            throw new UniqueResourceException("utilisateur.email.alreadyExists", email);
        }
        if (StringUtils.hasText(telephone) && repository.existsByTelephoneAndEntrepriseId(telephone, entrepriseId)) {
            throw new UniqueResourceException("utilisateur.telephone.alreadyExists", telephone);
        }
    }

    /** Variante update : exclut le fournisseur courant de la vérification. */
    public void ensureOptionalContactsForUpdateInEntreprise(String email, String telephone, UUID entrepriseId, UUID excludeId) {
        if (StringUtils.hasText(email) && repository.existsByEmailAndEntrepriseIdAndIdNot(email, entrepriseId, excludeId)) {
            throw new UniqueResourceException("utilisateur.email.alreadyExists", email);
        }
        if (StringUtils.hasText(telephone) && repository.existsByTelephoneAndEntrepriseIdAndIdNot(telephone, entrepriseId, excludeId)) {
            throw new UniqueResourceException("utilisateur.telephone.alreadyExists", telephone);
        }
    }
}
