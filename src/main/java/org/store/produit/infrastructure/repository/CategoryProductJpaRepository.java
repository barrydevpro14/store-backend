package org.store.produit.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.produit.domain.model.CategoryProduct;
import org.store.produit.domain.repository.CategoryProductRepository;

import java.util.UUID;

public interface CategoryProductJpaRepository extends JpaRepository<CategoryProduct, UUID>, CategoryProductRepository {
}
