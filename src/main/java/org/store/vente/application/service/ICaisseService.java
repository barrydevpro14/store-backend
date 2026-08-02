package org.store.vente.application.service;

import org.springframework.data.domain.Page;
import org.store.vente.application.dto.CaisseResumeFilter;
import org.store.vente.application.dto.CaisseResumeResponse;
import org.store.vente.application.dto.TopProduitResponse;
import org.store.vente.application.dto.TopProduitsFilter;

public interface ICaisseService {

    CaisseResumeResponse getResume(CaisseResumeFilter filter);

    Page<TopProduitResponse> findTopProduits(TopProduitsFilter filter);
}
