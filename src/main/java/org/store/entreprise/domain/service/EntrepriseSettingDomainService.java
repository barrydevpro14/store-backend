package org.store.entreprise.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.entreprise.domain.model.Entreprise;
import org.store.entreprise.domain.model.EntrepriseSetting;
import org.store.entreprise.domain.repository.EntrepriseSettingRepository;

import java.util.Optional;
import java.util.UUID;

@Service
public class EntrepriseSettingDomainService extends GlobalService<EntrepriseSetting, EntrepriseSettingRepository> {

    public EntrepriseSettingDomainService(EntrepriseSettingRepository repository) {
        super(repository);
    }

    public Optional<EntrepriseSetting> findByEntrepriseId(UUID entrepriseId) {
        return repository.findByEntrepriseId(entrepriseId);
    }

    public EntrepriseSetting upsert(Entreprise entreprise, String couleurPrimaire) {
        EntrepriseSetting setting = repository.findByEntrepriseId(entreprise.getId())
                .orElseGet(() -> {
                    EntrepriseSetting s = new EntrepriseSetting();
                    s.setEntreprise(entreprise);
                    return s;
                });

        setting.setCouleurPrimaire(couleurPrimaire);
        return save(setting);
    }
}
