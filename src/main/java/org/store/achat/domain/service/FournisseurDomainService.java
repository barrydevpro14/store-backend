package org.store.achat.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.store.achat.application.dto.FournisseurRequest;
import org.store.achat.application.dto.FournisseurResponse;
import org.store.achat.domain.model.Fournisseur;
import org.store.achat.domain.repository.FournisseurRepository;
import org.store.common.service.GlobalService;
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

    public Page<FournisseurResponse> findResponsesByEntrepriseId(UUID entrepriseId, Pageable pageable) {
        return repository.findResponsesByEntrepriseId(entrepriseId, pageable);
    }

    public Optional<Fournisseur> findByReferenceAndEntrepriseId(String reference, UUID entrepriseId) {
        return repository.findByReferenceAndEntrepriseId(reference, entrepriseId);
    }

    public boolean existsByReferenceAndEntrepriseId(String reference, UUID entrepriseId) {
        return repository.existsByReferenceAndEntrepriseId(reference, entrepriseId);
    }
}
