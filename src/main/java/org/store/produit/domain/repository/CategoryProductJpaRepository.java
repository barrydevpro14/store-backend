package org.store.produit.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.produit.domain.model.CategoryProduct;

import java.util.UUID;

public interface CategoryProductJpaRepository extends JpaRepository<CategoryProduct, UUID> {
}
