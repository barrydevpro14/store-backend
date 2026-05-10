package org.store.produit.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.produit.domain.model.ProductFournisseur;

import java.util.UUID;

public interface ProductFournisseurJpaRepository extends JpaRepository<ProductFournisseur, UUID> {
}
