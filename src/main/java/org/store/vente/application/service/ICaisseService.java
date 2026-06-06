package org.store.vente.application.service;

import org.store.vente.application.dto.CaisseResumeFilter;
import org.store.vente.application.dto.CaisseResumeResponse;
import org.store.vente.application.dto.TopProduitResponse;
import org.store.vente.application.dto.TopProduitsFilter;

import java.util.List;

public interface ICaisseService {

    CaisseResumeResponse getResume(CaisseResumeFilter filter);

    List<TopProduitResponse> findTopProduits(TopProduitsFilter filter);
}
