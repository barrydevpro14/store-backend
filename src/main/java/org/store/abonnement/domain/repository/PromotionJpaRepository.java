package org.store.abonnement.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.abonnement.domain.model.Promotion;

import java.util.UUID;

public interface PromotionJpaRepository extends JpaRepository<Promotion, UUID> {
}
