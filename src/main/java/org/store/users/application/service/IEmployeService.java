package org.store.users.application.service;

import org.springframework.data.domain.Page;
import org.store.users.application.dto.EmployeFilter;
import org.store.users.application.dto.EmployeRequest;
import org.store.users.application.dto.EmployeResponse;
import org.store.users.application.dto.EmployeUpdateRequest;
import org.store.users.domain.model.Employe;

import java.util.UUID;

public interface IEmployeService {

    EmployeResponse create(EmployeRequest employeRequest);

    /** Retourne l'Employe correspondant au user courant. Throw `ForbiddenException("vente.user.required")` si l'utilisateur connecté n'est pas un Employe. */
    Employe findCurrentUser();

    Page<EmployeResponse> findAllByCurrentEntreprise(EmployeFilter filter);

    EmployeResponse findResponseById(UUID id);

    EmployeResponse update(UUID id, EmployeUpdateRequest request);

    void deactivate(UUID id);

    void activate(UUID id);
}
