package org.store.security.application.service;

import org.store.security.application.dto.UserPrincipal;
import org.store.security.domain.model.Account;

public interface IUserPrincipalFactory {

    UserPrincipal build(Account account);
}
