package org.store.notification.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.notification.application.dto.AlerteResponse;
import org.store.notification.domain.enums.AlerteStatut;
import org.store.notification.domain.enums.AlerteType;
import org.store.notification.domain.model.Alerte;
import org.store.notification.domain.repository.AlerteRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AlerteDomainService extends GlobalService<Alerte, AlerteRepository> {

    public AlerteDomainService(AlerteRepository repository) {
        super(repository);
    }

    public Alerte create(AlerteType type, AlerteStatut statut, String titre, String message,
                         UUID entrepriseId, UUID magasinId, UUID entityId, Integer joursInfo) {
        Alerte alerte = new Alerte();
        alerte.setType(type);
        alerte.setStatut(statut);
        alerte.setTitre(titre);
        alerte.setMessage(message);
        alerte.setEntrepriseId(entrepriseId);
        alerte.setMagasinId(magasinId);
        alerte.setEntityId(entityId);
        alerte.setJoursInfo(joursInfo);
        return save(alerte);
    }

    private static final LocalDateTime SENTINEL_FROM = LocalDateTime.of(2000, 1, 1, 0, 0);
    private static final LocalDateTime SENTINEL_TO   = LocalDateTime.of(2099, 12, 31, 23, 59);

    public Page<AlerteResponse> findByFilter(UUID entrepriseId, UUID magasinId,
                                             AlerteType type, AlerteStatut statut,
                                             LocalDateTime from, LocalDateTime to,
                                             int page, int size) {
        String entrepriseIdStr = entrepriseId != null ? entrepriseId.toString() : "";
        String magasinIdStr    = magasinId    != null ? magasinId.toString()    : "";
        String typeStr         = type         != null ? type.name()             : "";
        String statutStr       = statut       != null ? statut.name()           : "";
        LocalDateTime fromDt   = from != null ? from : SENTINEL_FROM;
        LocalDateTime toDt     = to   != null ? to   : SENTINEL_TO;
        return repository.findByFilterNative(entrepriseIdStr, magasinIdStr, typeStr, statutStr, fromDt, toDt,
                        PageRequest.of(page, size))
                .map(AlerteResponse::new);
    }

    public Alerte markAsRead(Alerte alerte) {
        alerte.setStatut(AlerteStatut.LUE);
        return save(alerte);
    }

    public Alerte markAsResolved(Alerte alerte) {
        alerte.setStatut(AlerteStatut.RESOLUE);
        return save(alerte);
    }
}
