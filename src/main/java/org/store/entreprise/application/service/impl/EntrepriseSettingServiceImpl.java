package org.store.entreprise.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.entreprise.application.dto.EntrepriseSettingRequest;
import org.store.entreprise.application.dto.EntrepriseSettingResponse;
import org.store.entreprise.application.service.IEntrepriseSettingService;
import org.store.entreprise.domain.model.Entreprise;
import org.store.entreprise.domain.service.EntrepriseDomainService;
import org.store.entreprise.domain.service.EntrepriseSettingDomainService;
import org.store.security.application.service.ICurrentUserService;

import java.util.UUID;

/**
 * Manages per-enterprise PDF and display settings (e.g. primary colour).
 * Access is restricted to OWNER role via ENTREPRISE_ACCESS at the controller level.
 */
@Service
@Transactional(readOnly = true)
public class EntrepriseSettingServiceImpl implements IEntrepriseSettingService {

    private final EntrepriseSettingDomainService settingDomainService;
    private final EntrepriseDomainService entrepriseDomainService;
    private final ICurrentUserService currentUserService;

    public EntrepriseSettingServiceImpl(EntrepriseSettingDomainService settingDomainService,
                                        EntrepriseDomainService entrepriseDomainService,
                                        ICurrentUserService currentUserService) {
        this.settingDomainService = settingDomainService;
        this.entrepriseDomainService = entrepriseDomainService;
        this.currentUserService = currentUserService;
    }

    @Override
    public EntrepriseSettingResponse getMySettings() {
        UUID entrepriseId = currentUserService.getCurrent().entrepriseId();

        return settingDomainService.findByEntrepriseId(entrepriseId)
                .map(EntrepriseSettingResponse::new)
                .orElseGet(() -> new EntrepriseSettingResponse((String) null));
    }

    @Override
    @Transactional
    public EntrepriseSettingResponse updateMySettings(EntrepriseSettingRequest request) {
        UUID entrepriseId = currentUserService.getCurrent().entrepriseId();
        Entreprise entreprise = entrepriseDomainService.findById(entrepriseId);

        return new EntrepriseSettingResponse(settingDomainService.upsert(entreprise, request.couleurPrimaire()));
    }
}
