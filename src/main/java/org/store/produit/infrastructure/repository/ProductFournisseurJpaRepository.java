package org.store.produit.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.store.produit.domain.model.ProductFournisseur;
import org.store.produit.domain.repository.ProductFournisseurRepository;

import java.util.UUID;

@Repository
public interface ProductFournisseurJpaRepository extends JpaRepository<ProductFournisseur, UUID>, ProductFournisseurRepository {
}
