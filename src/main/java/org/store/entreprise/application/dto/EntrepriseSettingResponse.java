package org.store.entreprise.application.dto;

import org.store.entreprise.domain.model.EntrepriseSetting;

public record EntrepriseSettingResponse(String couleurPrimaire) {

    public EntrepriseSettingResponse(EntrepriseSetting setting) {
        this(setting.getCouleurPrimaire());
    }
}
