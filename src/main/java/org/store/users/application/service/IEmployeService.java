package org.store.users.application.service;

import org.store.users.application.dto.EmployeRequest;
import org.store.users.application.dto.EmployeResponse;
import org.store.users.domain.model.Employe;

public interface IEmployeService {

    EmployeResponse create(EmployeRequest employeRequest);

    /** Retourne l'Employe correspondant au user courant. Throw `ForbiddenException("vente.user.required")` si l'utilisateur connecté n'est pas un Employe. */
    Employe findCurrentUser();
}
