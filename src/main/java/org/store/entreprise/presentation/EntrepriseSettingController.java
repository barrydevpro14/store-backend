package org.store.entreprise.presentation;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.store.entreprise.application.dto.EntrepriseSettingRequest;
import org.store.entreprise.application.dto.EntrepriseSettingResponse;
import org.store.entreprise.application.service.IEntrepriseSettingService;

@RestController
@RequestMapping(EntrepriseSettingController.BASE_PATH)
public class EntrepriseSettingController {

    public static final String BASE_PATH = "/api/v1/entreprises/me/settings";

    private final IEntrepriseSettingService settingService;

    public EntrepriseSettingController(IEntrepriseSettingService settingService) {
        this.settingService = settingService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ENTREPRISE_SETTING_ACCESS')")
    public ResponseEntity<EntrepriseSettingResponse> getMySettings() {
        return ResponseEntity.ok(settingService.getMySettings());
    }

    @PutMapping
    @PreAuthorize("hasAuthority('ENTREPRISE_SETTING_ACCESS')")
    public ResponseEntity<EntrepriseSettingResponse> updateMySettings(@Valid @RequestBody EntrepriseSettingRequest request) {
        return ResponseEntity.ok(settingService.updateMySettings(request));
    }
}
