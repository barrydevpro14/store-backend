package org.store.users.application.service;

import org.store.users.application.dto.EmployeRequest;
import org.store.users.application.dto.EmployeResponse;

public interface IEmployeService {

    EmployeResponse create(EmployeRequest employeRequest);
}
