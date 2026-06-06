package org.store.produit.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.store.produit.domain.model.Product;
import org.store.produit.domain.repository.ProductRepository;

import java.util.UUID;

@Repository
public interface ProductJpaRepository extends JpaRepository<Product, UUID>, ProductRepository {
}
