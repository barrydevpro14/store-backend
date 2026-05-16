package org.store.vente.application.service;

import org.store.vente.application.dto.CaisseResumeFilter;
import org.store.vente.application.dto.CaisseResumeResponse;

public interface ICaisseService {

    CaisseResumeResponse getResume(CaisseResumeFilter filter);
}
