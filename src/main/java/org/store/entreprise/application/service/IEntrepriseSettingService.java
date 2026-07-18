package org.store.entreprise.application.service;

import org.store.entreprise.application.dto.EntrepriseSettingRequest;
import org.store.entreprise.application.dto.EntrepriseSettingResponse;

public interface IEntrepriseSettingService {
    EntrepriseSettingResponse getMySettings();
    EntrepriseSettingResponse updateMySettings(EntrepriseSettingRequest request);
}
