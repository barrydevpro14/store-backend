package org.store.produit.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.produit.domain.model.Product;

import java.util.UUID;

public interface ProductJpaRepository extends JpaRepository<Product, UUID> {
}
